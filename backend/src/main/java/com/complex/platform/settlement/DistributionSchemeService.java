package com.complex.platform.settlement;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.Hashing;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientResolver;
import com.complex.platform.merchantportal.MerchantZoneNoticeService;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionSchemeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.FirstSegmentInput;
import com.complex.platform.settlement.SettlementConfigWriteRequests.IncentiveApplicationRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.IncentiveDecisionRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.NoticePeriodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ParticipantTierInput;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 2 条：酒吧通票分账方案（参与家数档位表＋第一段名单）的草案 → 差人复核转正 → 通告 → 经通告期生效 → 版本。
 *
 * <p>依据：ENUM L52「参与家数档位表（出厂默认，业务参数、后台可配，须同时配置上下限）……不设家数上限……每一行的百分比合计必须等于 100%，
 * 合计不等于 100% 的配置不得生效」「档位表变更须提前向受影响的酒吧商户发出通告，自通告送达之日起经过通告期后方可生效……
 * 变更只对通告生效后新售出的票适用」「第一段的参与酒吧名单，以及第一段的固定金额或固定单价，其变更……同样须提前……通告……
 * 通告期与档位表变更同一参数」；PRD settlement-rebuild L294「第一段配置须与档位表共用同一套通告—生效—版本固化机制（不新建第二套）」。
 * 送达口径按操作员裁决（第十八批第 1 条）：复核转正当日系统逐家发出通告即视为送达，生效日＝转正日＋通告期；
 * 通告渠道按 18B 复核后裁决第 10 条「商户专区显示通告」：逐家挂进受影响酒吧的商户专区＝送达，短信照旧可关、只作提醒。
 *
 * <p>第 75 条（ENUM L52、BSM L752）：复核转正前按最低可能可分配池金额逐票种试算第一段（{@link PassPoolTrialService}），不通过且没有已批准的
 * 短期经营激励申请即不得转正；短期经营激励申请（首次、到期重审）与项目之上的平台权限审批也在本服务。
 * 激励到期未重新获批按 18C 核对后操作员裁决补充第 12 条「整版回到此前方案（推荐）：到期那天起新卖的票改按此前在用的方案（本项目上一版，
 * 没有就用全平台版）；已卖的票照售票时固化的结果结算，不追溯；到期日和「到期未重新获批就改回此前方案」在转正当天的商户专区通告里一并写明」。
 */
@Service
public class DistributionSchemeService {

    private static final String PERM = "settlement.config.manage";
    private static final String TABLE = "ticket_distribution_scheme_versions";
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_NOTIFIED = "notified";
    private static final String INCENTIVE_TABLE = "pass_incentive_applications";
    private static final String INCENTIVE_APPROVE_PERM = "settlement.bill.special_approve";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditEventService audit;
    private final NotificationService notifications;
    private final MerchantFeeNotificationRecipientResolver recipients;
    private final BusinessDayResolver businessDays;
    private final MerchantZoneNoticeService zoneNotices;
    private final PassPoolTrialService poolTrials;
    private final JdbcSettlementBillRepository bills;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public DistributionSchemeService(NamedParameterJdbcTemplate jdbc, AuditEventService audit,
                                     NotificationService notifications, MerchantFeeNotificationRecipientResolver recipients,
                                     BusinessDayResolver businessDays, MerchantZoneNoticeService zoneNotices,
                                     PassPoolTrialService poolTrials, JdbcSettlementBillRepository bills,
                                     com.fasterxml.jackson.databind.ObjectMapper json) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.notifications = notifications;
        this.recipients = recipients;
        this.businessDays = businessDays;
        this.zoneNotices = zoneNotices;
        this.poolTrials = poolTrials;
        this.bills = bills;
        this.json = json;
    }

    /** 参与家数档位表一格（万分比）。上下限出厂留空，配置时须同时填写。 */
    public record SchemeTier(int participantCount, int rankNo, int shareBps, Integer minBps, Integer maxBps) {
    }

    /** 第一段名单一行：rule_type＝fixed（固定金额）或 per_guest（固定单价），金额均按每份票（每人每次入场一份）计。 */
    public record FirstSegmentRow(String merchantId, String ruleType, Long perGuestCent, Long fixedAmountCent) {
        long amountCent() {
            return JdbcSettlementBillRepository.RULE_FIXED.equals(ruleType) ? fixedAmountCent : perGuestCent;
        }
    }

    /**
     * state：draft 草案 / pending 已通告待生效 / effective 现行 / superseded 已被后续版本取代 /
     * incentive_expired 凭短期经营激励转正、激励已到期（裁决第 12 条：到期营业日起新售出的票改按此前在用的方案，本版不再是现行版）。
     */
    public record SchemeVersion(String id, String projectId, String status, String state, String draftedBy, String approvedBy,
                                Instant notifiedAt, Instant effectiveAt, List<SchemeTier> tiers,
                                List<FirstSegmentRow> firstSegment) {
    }

    public record NoticePeriod(int periodValue, String periodUnit, Integer minValue, Integer maxValue) {
    }

    /** 「补配该家数档位行」待办：某项目缺某家数行而暂挂的份数与第二段余额合计。 */
    public record MissingRow(String projectId, int participantCount, int units, long amountCent) {
    }

    /**
     * 第 75 条短期经营激励申请：kind＝initial（配置生效前提交，转正试算不通过时凭已批准的这一笔放行）或 renewal（到期重审）；
     * trialResult＝提交时逐票种试算（票种、最低实付价、最不利一份可分配池、第一段应分合计、超出额，分）；
     * 有效期在该版转正（首次）或重审获批时定。
     */
    public record IncentiveApplication(String id, String schemeVersionId, String projectId, String kind, String status,
                                       String reason, int termValue, String termUnit, long maxExcessCent,
                                       List<Map<String, Object>> trialResult, String submittedBy, Instant submittedAt,
                                       String decidedBy, Instant decidedAt, String decisionNote, Instant validFrom,
                                       Instant validUntil) {
    }

    public record SchemeOverview(NoticePeriod noticePeriod, List<SchemeVersion> versions, List<MissingRow> missingRows,
                                 List<IncentiveApplication> incentiveApplications) {
    }

    // ================= 写入 =================

    /** 整套草案（档位表＋第一段名单）落库；校验不过 400；同一幂等键重放返回原草案。 */
    public String saveDraft(DistributionSchemeRequest request, String actorId) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        String idem = requireText(request.idempotencyKey(), "idempotency_key");
        String projectId = trimToNull(request.projectId());
        List<SchemeTier> tiers = toTierRows(request.tiers());
        validateTiers(tiers, true);
        List<FirstSegmentRow> firstSegment = toFirstSegmentRows(request.firstSegment());
        String scope = projectId == null ? "__DEFAULT__" : projectId;
        String id = "tdsv_" + Hashing.idempotencyCode(scope + "|" + idem);
        int inserted = jdbc.update("""
                INSERT INTO ticket_distribution_scheme_versions (id, project_id, status, drafted_by)
                VALUES (:id, :project_id, 'draft', :actor) ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", id).addValue("project_id", projectId).addValue("actor", actorId));
        if (inserted == 0) {
            return id;
        }
        for (SchemeTier t : tiers) {
            jdbc.update("""
                    INSERT INTO ticket_distribution_participant_tiers
                        (version_id, participant_count, rank_no, share_bps, min_bps, max_bps)
                    VALUES (:v, :n, :r, :s, :min, :max)
                    """, new MapSqlParameterSource().addValue("v", id).addValue("n", t.participantCount())
                    .addValue("r", t.rankNo()).addValue("s", t.shareBps()).addValue("min", t.minBps()).addValue("max", t.maxBps()));
        }
        for (FirstSegmentRow f : firstSegment) {
            jdbc.update("""
                    INSERT INTO ticket_distribution_first_segment_rows (version_id, merchant_id, rule_type, per_guest_cent, fixed_amount_cent)
                    VALUES (:v, :m, :t, :pg, :fx)
                    """, new MapSqlParameterSource().addValue("v", id).addValue("m", f.merchantId()).addValue("t", f.ruleType())
                    .addValue("pg", f.perGuestCent(), java.sql.Types.BIGINT)
                    .addValue("fx", f.fixedAmountCent(), java.sql.Types.BIGINT));
        }
        audit.recordSuccess("settlement.config.distribution_scheme.updated", PERM, TABLE, id, null, idem,
                Map.of("project_scope", scope, "tier_cells", tiers.size(), "first_segment_rows", firstSegment.size()));
        return id;
    }

    /**
     * 差人复核转正：复校每个家数行合计 100%，置已通告，生效日＝转正当日＋通告期（裁决第十八批第 1 条），并在同一事务里逐家把通告挂进
     * 受影响酒吧的商户专区（18B 复核后裁决第 10 条，挂出即送达）。
     * 转正日与生效日都按营业日（ENUM L187「营业日从当日上午 10:00 起算，至次日上午 10:00 止」，按方案所属项目，全平台方案按出厂起点）：
     * 生效时刻＝生效营业日的起点。
     * 受影响酒吧＝本范围已发布通票（非单店票）入场类权益覆盖的全部酒吧 ∪ 本版第一段名单——第一段变动会改变第二段余额，档位变动影响全部第二段酒吧；
     * 全平台版本不含所在项目已有不晚于本版生效的本项目版的酒吧（本版对其不生效）。
     * 第 75 条（ENUM L52「第一段的固定金额或固定单价在配置生效之前，必须先按该票种……最低可能可分配池金额试算……试算不通过的配置无效、
     * 不得生效」「娱乐平台公司确需让第一段合计高于池金额的，须在配置生效前提交短期经营激励申请并经项目之上的平台权限审批」）：转正即定生效日
     * 并逐家通告，是配置生效前的最后一道闸——任一受试票种不通过、且本版没有已批准的首次激励申请即 409 POOL_TRIAL_FAILED、状态不变、不发通告；
     * 凭激励放行的，该申请有效期自本版生效营业日起算（激励的内容是让第一段合计高于池金额，这件事从该版生效才发生），
     * 激励到期日与「到期未重新获批的，自该日起新售出的票改按此前在用的方案」写进本次通告（裁决第 12 条，不另发通告与短信）。
     */
    @Transactional
    public SchemeVersion activate(String versionId, String actorId) {
        Map<String, Object> row = jdbc.queryForList(
                "SELECT status, drafted_by, project_id FROM ticket_distribution_scheme_versions WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", versionId)).stream().findFirst().orElse(null);
        if (row == null || !STATUS_DRAFT.equals(row.get("status"))) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "草案不存在或非草案态,无法转正");
        }
        if (actorId != null && actorId.equals(row.get("drafted_by"))) {
            throw ApiException.forbidden("SELF_ACTIVATION_FORBIDDEN", "不能自行转正/软删自己起草的改动,须另一高权限人复核批准");
        }
        validateTiers(tierRows(versionId), false);
        String scopeProject = (String) row.get("project_id");
        LocalDate effectiveDate = pendingEffectiveDate(scopeProject);
        List<PassPoolTrialService.TicketTypeTrial> trials = poolTrials.trial(scopeProject, effectiveDate, firstSegment(versionId));
        List<PassPoolTrialService.TicketTypeTrial> failed = trials.stream().filter(t -> !t.passed()).toList();
        Map<String, Object> incentive = failed.isEmpty() ? null : jdbc.queryForList("""
                SELECT id, term_value, term_unit FROM pass_incentive_applications
                 WHERE scheme_version_id = :v AND kind = 'initial' AND status = 'approved'
                 ORDER BY decided_at DESC, id LIMIT 1
                """, new MapSqlParameterSource("v", versionId)).stream().findFirst().orElse(null);
        if (!failed.isEmpty() && incentive == null) {
            throw ApiException.conflict("POOL_TRIAL_FAILED", trialFailureMessage(failed));
        }
        Timestamp effectiveAt = Timestamp.from(businessDays.start(scopeProject, null, effectiveDate));
        jdbc.update("""
                UPDATE ticket_distribution_scheme_versions
                   SET status = 'notified', approved_by = :actor, notified_at = now(), effective_at = :effective_at
                 WHERE id = :id AND status = 'draft'
                """, new MapSqlParameterSource().addValue("id", versionId).addValue("actor", actorId)
                .addValue("effective_at", effectiveAt));
        Map<String, Object> poolTrial = new LinkedHashMap<>();
        poolTrial.put("tested", trials.size());
        poolTrial.put("failed", failed.size());
        LocalDate incentiveUntil = incentive == null ? null : term(incentive).after(effectiveDate);
        if (incentive != null) {
            jdbc.update("UPDATE pass_incentive_applications SET valid_from = :from, valid_until = :until WHERE id = :id",
                    new MapSqlParameterSource().addValue("id", incentive.get("id")).addValue("from", effectiveAt)
                            .addValue("until", Timestamp.from(businessDays.start(scopeProject, null, incentiveUntil))));
            poolTrial.put("incentive_application_id", incentive.get("id"));
        }
        audit.recordSuccess("settlement.config.draft.activated", PERM, TABLE, versionId, null, null,
                Map.of("config_group", TABLE, "effective_date", effectiveDate.toString(), "pool_trial", poolTrial));
        notifyAffectedBars(versionId, (String) row.get("project_id"), effectiveDate, incentiveUntil);
        return overview().versions().stream().filter(v -> v.id().equals(versionId)).findFirst().orElseThrow();
    }

    /** 通告期：数值、单位与上下限同时必填，1 ≤ 下限 ≤ 数值 ≤ 上限。 */
    public NoticePeriod saveNoticePeriod(NoticePeriodRequest request, String actorId) {
        requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        if (request.periodValue() == null || request.minValue() == null || request.maxValue() == null
                || !Set.of("day", "month").contains(request.periodUnit()) || request.minValue() < 1
                || request.periodValue() < request.minValue() || request.periodValue() > request.maxValue()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "通告期须同时填写数值、单位（天/月）及上下限，下限不小于 1，数值须在上下限内");
        }
        jdbc.update("""
                UPDATE ticket_distribution_notice_period
                   SET period_value = :v, period_unit = :u, min_value = :min, max_value = :max, updated_by = :actor, updated_at = now()
                 WHERE id = 'default'
                """, new MapSqlParameterSource().addValue("v", request.periodValue()).addValue("u", request.periodUnit())
                .addValue("min", request.minValue()).addValue("max", request.maxValue()).addValue("actor", actorId));
        audit.recordSuccess("settlement.config.distribution_notice_period.updated", PERM, "ticket_distribution_notice_period",
                "default", null, request.idempotencyKey(), Map.of("period_value", request.periodValue(),
                        "period_unit", request.periodUnit(), "min_value", request.minValue(), "max_value", request.maxValue()));
        return noticePeriod();
    }

    /**
     * 第 75 条短期经营激励申请（ENUM L52「娱乐平台公司确需让第一段合计高于池金额的，须在配置生效前提交短期经营激励申请……初始有效期不超过
     * 后台配置的上限……到期重审」；BSM L752「单次激励期限不得超过后台配置的上限……期限届满须重新提交审批，不得自动续期」）：
     * 草案版本提交的是首次申请（配置生效前）；已凭激励放行转正的版本提交的是重审。提交时按转正同一口径重跑试算，留存逐票种结果与最大超出额；
     * 期限换成同一参照日的到期日后不得超过本版项目的激励期限上限（项目特例＞全局默认）。系统没有娱乐平台公司员工归属，由结算配置管理人员代提交。
     * 激励已到期的版本不再受理重审（裁决第 12、13 条：到期营业日起已改回此前在用的方案）。
     * 同一幂等键重放返回原申请。
     */
    @Transactional
    public IncentiveApplication submitIncentive(String versionId, IncentiveApplicationRequest request, String actorId) {
        String idem = requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        String id = "pia_" + Hashing.idempotencyCode(versionId + "|" + idem);
        IncentiveApplication existing = findIncentive(id);
        if (existing != null) {
            return existing;
        }
        String reason = requireText(request.reason(), "reason");
        if (request.termValue() == null || request.termValue() < 1 || !Set.of("day", "month").contains(request.termUnit())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "激励期限须填写数值（term_value，不小于 1）与单位（term_unit：天或月）");
        }
        Map<String, Object> version = jdbc.queryForList(
                "SELECT status, project_id, effective_at FROM ticket_distribution_scheme_versions WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", versionId)).stream().findFirst().orElse(null);
        if (version == null) {
            throw ApiException.notFound("DISTRIBUTION_SCHEME_NOT_FOUND", "通票分账方案版本不存在");
        }
        String project = (String) version.get("project_id");
        boolean initial = STATUS_DRAFT.equals(version.get("status"));
        if (!initial) {
            Timestamp until = incentiveUntil(versionId);
            if (until == null) {
                throw ApiException.conflict("INCENTIVE_RENEWAL_NOT_APPLICABLE", "该版本未凭短期经营激励转正，没有可重审的激励");
            }
            requireIncentiveNotLapsed(project, until);
        }
        JdbcSettlementBillRepository.SpecialApprovalTerm term =
                new JdbcSettlementBillRepository.SpecialApprovalTerm(request.termValue(), request.termUnit());
        JdbcSettlementBillRepository.SpecialApprovalTerm limit = bills.findSpecialApprovalTerm(project);
        LocalDate today = businessDays.businessDate(project, null, Instant.now());
        if (term.after(today).isAfter(limit.after(today))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "激励期限（term_value）" + term.label()
                    + " 超过本项目短期经营激励单次有效期上限 " + limit.label());
        }
        LocalDate effectiveDate = initial ? pendingEffectiveDate(project)
                : businessDays.businessDate(project, null, ((Timestamp) version.get("effective_at")).toInstant());
        List<Map<String, Object>> trialRows = new ArrayList<>();
        long maxExcess = 0L;
        for (PassPoolTrialService.TicketTypeTrial t : poolTrials.trial(project, effectiveDate, firstSegment(versionId))) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ticket_type_id", t.ticketTypeId());
            r.put("ticket_name", t.ticketName());
            r.put("lowest_price_cent", t.lowestPriceCent());
            r.put("lowest_unit_pool_cent", t.lowestUnitPoolCent());
            r.put("first_segment_cent", t.firstSegmentCent());
            r.put("excess_cent", t.excessCent());
            trialRows.add(r);
            maxExcess = Math.max(maxExcess, t.excessCent());
        }
        String kind = initial ? "initial" : "renewal";
        jdbc.update("""
                INSERT INTO pass_incentive_applications (id, scheme_version_id, project_id, kind, status, reason, term_value, term_unit,
                    trial_result, max_excess_cent, submitted_by)
                VALUES (:id, :v, :p, :kind, 'pending', :reason, :tv, :tu, CAST(:trial AS jsonb), :excess, :actor)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", id).addValue("v", versionId).addValue("p", project)
                .addValue("kind", kind).addValue("reason", reason).addValue("tv", term.value()).addValue("tu", term.unit())
                .addValue("trial", writeJson(trialRows)).addValue("excess", maxExcess).addValue("actor", actorId));
        audit.recordSuccess("settlement.config.pass_incentive.submitted", PERM, INCENTIVE_TABLE, id, null, idem,
                Map.of("scheme_version_id", versionId, "kind", kind, "max_excess_cent", maxExcess, "term", term.label()));
        return findIncentive(id);
    }

    /**
     * 第 75 条审批（DEC-R3-2026-08-04-13：审批人为项目之上的平台权限，权限与范围由控制器前置校验）：只审待审申请；审批人不得是提交人
     * （涉钱配置差人复核）。重审获批从上一笔已批准激励的到期日接续一个新期限（BSM L752「不得自动续期」——没有任何自动续期作业）；
     * 上一笔已到期才来批准的重审不批（裁决第 12、13 条：到期营业日起已改回此前在用的方案）；
     * 首次申请的有效期在该版转正时定。同一人以同一决定重放返回原结果。
     */
    @Transactional
    public IncentiveApplication decideIncentive(String applicationId, boolean approve, IncentiveDecisionRequest request,
                                                String actorId) {
        String idem = requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
        Map<String, Object> row = jdbc.queryForList("""
                SELECT scheme_version_id, project_id, kind, status, submitted_by, decided_by, term_value, term_unit
                  FROM pass_incentive_applications WHERE id = :id FOR UPDATE
                """, new MapSqlParameterSource("id", applicationId)).stream().findFirst().orElse(null);
        if (row == null) {
            throw ApiException.notFound("INCENTIVE_APPLICATION_NOT_FOUND", "短期经营激励申请不存在");
        }
        String decision = approve ? "approved" : "rejected";
        if (!"pending".equals(row.get("status"))) {
            if (decision.equals(row.get("status")) && actorId != null && actorId.equals(row.get("decided_by"))) {
                return findIncentive(applicationId);
            }
            throw ApiException.conflict("INCENTIVE_APPLICATION_NOT_PENDING", "该申请已审批，不能再审");
        }
        if (actorId != null && actorId.equals(row.get("submitted_by"))) {
            throw ApiException.forbidden("SELF_ACTIVATION_FORBIDDEN", "不能审批自己提交的短期经营激励申请，须另一位有平台审批权限的人审批");
        }
        Timestamp from = null;
        Timestamp until = null;
        if (approve && "renewal".equals(row.get("kind"))) {
            String project = (String) row.get("project_id");
            from = incentiveUntil((String) row.get("scheme_version_id"));
            requireIncentiveNotLapsed(project, from);
            until = Timestamp.from(businessDays.start(project, null,
                    term(row).after(businessDays.businessDate(project, null, from.toInstant()))));
        }
        jdbc.update("""
                UPDATE pass_incentive_applications
                   SET status = :s, decided_by = :actor, decided_at = now(), decision_note = :note, valid_from = :from, valid_until = :until
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", applicationId).addValue("s", decision).addValue("actor", actorId)
                .addValue("note", trimToNull(request.note())).addValue("from", from, java.sql.Types.TIMESTAMP)
                .addValue("until", until, java.sql.Types.TIMESTAMP));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("scheme_version_id", row.get("scheme_version_id"));
        meta.put("kind", row.get("kind"));
        meta.put("term", term(row).label());
        if (until != null) {
            meta.put("valid_from", from.toInstant().toString());
            meta.put("valid_until", until.toInstant().toString());
        }
        audit.recordSuccess("settlement.config.pass_incentive." + decision, INCENTIVE_APPROVE_PERM, INCENTIVE_TABLE,
                applicationId, null, idem, meta);
        return findIncentive(applicationId);
    }

    // ================= 读取 =================

    /** 各版本（状态、通告日、生效日、档位行与第一段名单）、通告期，以及「补配该家数档位行」待办。 */
    public SchemeOverview overview() {
        Instant now = Instant.now();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT v.id, v.project_id, v.status, v.drafted_by, v.approved_by, v.notified_at, v.effective_at,
                       (SELECT MAX(i.valid_until) FROM pass_incentive_applications i
                         WHERE i.scheme_version_id = v.id AND i.status = 'approved') AS incentive_until
                  FROM ticket_distribution_scheme_versions v ORDER BY v.project_id NULLS FIRST, v.created_at DESC, v.id
                """, new MapSqlParameterSource());
        // 各范围现行版：已通告、已到生效时刻且激励未到期（裁决第 12 条）的最晚一版。
        Map<String, Instant> currentAt = new LinkedHashMap<>();
        Map<String, String> currentId = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Instant at = instant(r.get("effective_at"));
            String scope = String.valueOf(r.get("project_id"));
            if (STATUS_NOTIFIED.equals(r.get("status")) && !at.isAfter(now) && !lapsed(r, now)
                    && (!currentAt.containsKey(scope) || at.isAfter(currentAt.get(scope)))) {
                currentAt.put(scope, at);
                currentId.put(scope, (String) r.get("id"));
            }
        }
        List<SchemeVersion> versions = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("id");
            Instant at = instant(r.get("effective_at"));
            String state = STATUS_DRAFT.equals(r.get("status")) ? "draft"
                    : at.isAfter(now) ? "pending"
                    : lapsed(r, now) ? "incentive_expired"
                    : id.equals(currentId.get(String.valueOf(r.get("project_id")))) ? "effective" : "superseded";
            versions.add(new SchemeVersion(id, (String) r.get("project_id"), (String) r.get("status"), state,
                    (String) r.get("drafted_by"), (String) r.get("approved_by"), instant(r.get("notified_at")), at,
                    tierRows(id), firstSegment(id)));
        }
        List<MissingRow> missing = jdbc.query("""
                SELECT u.project_id, u.n, COUNT(*) AS units, COALESCE(SUM(u.p2), 0) AS amount
                  FROM (SELECT b.project_id, r.ticket_instance_id, r.slot_index, r.visit_no,
                               COUNT(*) AS n, MAX(r.segment_pool_cent) AS p2
                          FROM pass_pool_unit_results r
                          JOIN settlement_bills b ON b.id = r.bill_id
                          JOIN pass_pool_snapshots ps ON ps.ticket_instance_id = r.ticket_instance_id AND ps.status = 'active'
                         WHERE r.status = 'suspended'
                           AND NOT EXISTS (SELECT 1 FROM pass_pool_unit_results x
                                            WHERE x.status = 'resumed' AND x.ticket_instance_id = r.ticket_instance_id
                                              AND x.slot_index = r.slot_index AND x.visit_no = r.visit_no)
                         GROUP BY 1, 2, 3, 4) u
                 GROUP BY 1, 2
                 ORDER BY 1 NULLS FIRST, 2
                """, new MapSqlParameterSource(), (rs, i) -> new MissingRow(rs.getString("project_id"), rs.getInt("n"),
                rs.getInt("units"), rs.getLong("amount")));
        return new SchemeOverview(noticePeriod(), versions, missing, incentives("ORDER BY submitted_at DESC, id", new MapSqlParameterSource()));
    }

    /**
     * 时刻 at 售出的票所用方案版本：按该项目的售票营业日（ENUM L187）与各版生效营业日比较——本项目有已生效版本取本项目最晚一版，
     * 否则取全平台最晚一版（出厂版恒在）。全平台版本在各项目按各自营业日切换。
     * 凭激励转正的版本自激励到期营业日起不再取用（裁决第 12 条），新售票落到此前在用的版本（本项目上一版，没有就用全平台版）；
     * 已售票按池快照固化的版本与第一段结算，不经此处。
     */
    public String effectiveVersionId(String projectId, Instant at) {
        return jdbc.queryForList("""
                SELECT v.id FROM ticket_distribution_scheme_versions v
                 WHERE v.status = 'notified' AND (v.effective_at AT TIME ZONE :zone)::date <= :sale_day
                   AND (v.project_id IS NULL OR v.project_id = :project_id)
                   AND %s > :sale_day
                 ORDER BY (v.project_id IS NOT NULL) DESC, v.effective_at DESC, v.notified_at DESC
                 LIMIT 1
                """.formatted(incentiveExpiryDate("v")),
                new MapSqlParameterSource().addValue("zone", LocalZone.ID.getId())
                        .addValue("sale_day", java.sql.Date.valueOf(businessDays.businessDate(projectId, null, at)))
                        .addValue("project_id", projectId),
                String.class).stream().findFirst().orElseThrow(() -> ApiException.badRequest("SETTLEMENT_CONFIG_MISSING",
                        "结算配置缺失且无默认种子:" + TABLE + ";请先在结算配置中心配置并转正"));
    }

    List<FirstSegmentRow> firstSegment(String versionId) {
        return jdbc.query("""
                SELECT merchant_id, rule_type, per_guest_cent, fixed_amount_cent FROM ticket_distribution_first_segment_rows
                 WHERE version_id = :v ORDER BY merchant_id
                """, new MapSqlParameterSource("v", versionId), (rs, i) -> new FirstSegmentRow(rs.getString("merchant_id"),
                rs.getString("rule_type"), (Long) rs.getObject("per_guest_cent"), (Long) rs.getObject("fixed_amount_cent")));
    }

    private List<SchemeTier> tierRows(String versionId) {
        return jdbc.query("""
                SELECT participant_count, rank_no, share_bps, min_bps, max_bps FROM ticket_distribution_participant_tiers
                 WHERE version_id = :v ORDER BY participant_count, rank_no
                """, new MapSqlParameterSource("v", versionId), (rs, i) -> new SchemeTier(rs.getInt("participant_count"),
                rs.getInt("rank_no"), rs.getInt("share_bps"), (Integer) rs.getObject("min_bps"), (Integer) rs.getObject("max_bps")));
    }

    private NoticePeriod noticePeriod() {
        return jdbc.queryForObject("""
                SELECT period_value, period_unit, min_value, max_value FROM ticket_distribution_notice_period WHERE id = 'default'
                """, new MapSqlParameterSource(), (rs, i) -> new NoticePeriod(rs.getInt("period_value"), rs.getString("period_unit"),
                (Integer) rs.getObject("min_value"), (Integer) rs.getObject("max_value")));
    }

    private IncentiveApplication findIncentive(String id) {
        return incentives("WHERE id = :id", new MapSqlParameterSource("id", id)).stream().findFirst().orElse(null);
    }

    private List<IncentiveApplication> incentives(String clause, MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT id, scheme_version_id, project_id, kind, status, reason, term_value, term_unit, max_excess_cent,
                       trial_result::text AS trial_result, submitted_by, submitted_at, decided_by, decided_at, decision_note,
                       valid_from, valid_until
                  FROM pass_incentive_applications
                """ + clause, params, (rs, i) -> new IncentiveApplication(rs.getString("id"), rs.getString("scheme_version_id"),
                rs.getString("project_id"), rs.getString("kind"), rs.getString("status"), rs.getString("reason"),
                rs.getInt("term_value"), rs.getString("term_unit"), rs.getLong("max_excess_cent"), readJson(rs.getString("trial_result")),
                rs.getString("submitted_by"), instant(rs.getTimestamp("submitted_at")), rs.getString("decided_by"),
                instant(rs.getTimestamp("decided_at")), rs.getString("decision_note"), instant(rs.getTimestamp("valid_from")),
                instant(rs.getTimestamp("valid_until"))));
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("试算结果序列化失败", e);
        }
    }

    private List<Map<String, Object>> readJson(String text) {
        try {
            return json.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("试算结果解析失败", e);
        }
    }

    /** 本版若此刻转正的生效营业日＝转正营业日＋通告期（裁决第十八批第 1 条；按方案所属项目，全平台方案按出厂起点）。 */
    private LocalDate pendingEffectiveDate(String scopeProject) {
        NoticePeriod period = noticePeriod();
        LocalDate today = businessDays.businessDate(scopeProject, null, Instant.now());
        return "day".equals(period.periodUnit()) ? today.plusDays(period.periodValue()) : today.plusMonths(period.periodValue());
    }

    /**
     * 凭激励转正的版本（有已批准且已定有效期的激励申请）最后一笔已批准激励的到期营业日，SQL date 表达式（无则 infinity），须绑定 :zone。
     * 裁决第 12 条：售票取版本以「到期营业日晚于售票营业日」作为该版仍在用；方案通告与转正试算里「所在项目已有本项目版」只算
     * 无激励到期日（infinity）的本项目版——凭激励转正的本项目版到期后新售票会落回此前方案（没有本项目上一版即全平台版）。
     */
    static String incentiveExpiryDate(String alias) {
        return "COALESCE((SELECT (MAX(i.valid_until) AT TIME ZONE :zone)::date FROM pass_incentive_applications i"
                + " WHERE i.scheme_version_id = " + alias + ".id AND i.status = 'approved'), 'infinity'::date)";
    }

    /** 该版最后一笔已批准激励的到期时刻（到期营业日起点）；没有凭激励转正即 null。 */
    private Timestamp incentiveUntil(String versionId) {
        return jdbc.queryForObject("""
                SELECT MAX(valid_until) FROM pass_incentive_applications WHERE scheme_version_id = :v AND status = 'approved'
                """, new MapSqlParameterSource("v", versionId), Timestamp.class);
    }

    /** 裁决第 12、13 条：激励已到期即已改回此前在用的方案，不再受理重审，要继续只能起草新版走试算、申请与通告期。 */
    private void requireIncentiveNotLapsed(String project, Timestamp until) {
        if (!Instant.now().isBefore(until.toInstant())) {
            throw ApiException.conflict("INCENTIVE_EXPIRED", "该版短期经营激励已于 "
                    + businessDays.businessDate(project, null, until.toInstant()).format(CN_DATE)
                    + " 到期，自该日起新售出的票已改按此前在用的方案，不再受理重审；要继续须起草新版，重新试算、申请短期经营激励并经通告期生效");
        }
    }

    /** 总览行：凭激励转正且此刻已到激励到期营业日起点。 */
    private static boolean lapsed(Map<String, Object> row, Instant now) {
        Instant until = instant(row.get("incentive_until"));
        return until != null && !now.isBefore(until);
    }

    private static JdbcSettlementBillRepository.SpecialApprovalTerm term(Map<String, Object> application) {
        return new JdbcSettlementBillRepository.SpecialApprovalTerm(((Number) application.get("term_value")).intValue(),
                (String) application.get("term_unit"));
    }

    /** 逐个未通过的票种写明金额（元），让起草人知道差多少、该改第一段还是走激励申请。 */
    private static String trialFailureMessage(List<PassPoolTrialService.TicketTypeTrial> failed) {
        List<String> parts = new ArrayList<>();
        for (PassPoolTrialService.TicketTypeTrial t : failed) {
            parts.add("票种「" + t.ticketName() + "」：最低实付价 " + yuan(t.lowestPriceCent()) + " 元，最不利一份可分配池 "
                    + yuan(t.lowestUnitPoolCent()) + " 元，第一段应分合计 " + yuan(t.firstSegmentCent()) + " 元，超出 "
                    + yuan(t.excessCent()) + " 元");
        }
        return "第一段试算不通过，配置不得生效：" + String.join("；", parts) + "。确需超池须先提交短期经营激励申请并经平台审批";
    }

    private static String yuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    // ================= 校验与通告 =================

    /**
     * 每个家数行：名次恰为 1..N、合计 10000 基点（「合计不等于 100% 的配置不得生效」）；不设家数上限。
     * requireBounds＝落草案时每格上下限须同时填写且比例在上下限内（「须同时配置上下限」）。
     */
    private static void validateTiers(List<SchemeTier> tiers, boolean requireBounds) {
        if (tiers.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "参与家数档位表不能为空");
        }
        Map<Integer, List<SchemeTier>> byCount = new TreeMap<>();
        for (SchemeTier t : tiers) {
            if (t.participantCount() < 1 || t.shareBps() < 0 || t.shareBps() > 10000) {
                throw ApiException.badRequest("VALIDATION_FAILED", "第二段参与家数须不小于 1，各名次比例须在 0% 到 100% 之间");
            }
            if (requireBounds && (t.minBps() == null || t.maxBps() == null
                    || t.minBps() < 0 || t.maxBps() > 10000 || t.minBps() > t.shareBps() || t.shareBps() > t.maxBps())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "第二段参与 " + t.participantCount() + " 家第 " + t.rankNo()
                        + " 名须同时配置上下限，且比例在上下限之内");
            }
            byCount.computeIfAbsent(t.participantCount(), k -> new ArrayList<>()).add(t);
        }
        for (Map.Entry<Integer, List<SchemeTier>> e : byCount.entrySet()) {
            int n = e.getKey();
            Set<Integer> ranks = new HashSet<>();
            long sum = 0L;
            for (SchemeTier t : e.getValue()) {
                ranks.add(t.rankNo());
                sum += t.shareBps();
            }
            if (e.getValue().size() != n || ranks.size() != n || ranks.stream().anyMatch(r -> r < 1 || r > n)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "第二段参与 " + n + " 家的档位行须恰好配置第 1 至第 " + n + " 名各一格");
            }
            if (sum != 10000L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "第二段参与 " + n + " 家的档位行合计为 "
                        + BigDecimal.valueOf(sum, 2).stripTrailingZeros().toPlainString() + "%，须等于 100%");
            }
        }
    }

    private static List<SchemeTier> toTierRows(List<ParticipantTierInput> inputs) {
        List<SchemeTier> out = new ArrayList<>();
        for (ParticipantTierInput in : inputs == null ? List.<ParticipantTierInput>of() : inputs) {
            if (in == null || in.participantCount() == null || in.rankNo() == null || in.shareBps() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "档位行须填写第二段参与家数、名次与比例");
            }
            out.add(new SchemeTier(in.participantCount(), in.rankNo(), in.shareBps(), in.minBps(), in.maxBps()));
        }
        return out;
    }

    private static List<FirstSegmentRow> toFirstSegmentRows(List<FirstSegmentInput> inputs) {
        List<FirstSegmentRow> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (FirstSegmentInput in : inputs == null ? List.<FirstSegmentInput>of() : inputs) {
            String merchant = in == null ? null : trimToNull(in.merchantId());
            if (merchant == null || !seen.add(merchant) || (in.perGuestCent() == null) == (in.fixedAmountCent() == null)
                    || (in.perGuestCent() != null && in.perGuestCent() < 0)
                    || (in.fixedAmountCent() != null && in.fixedAmountCent() < 0)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "第一段名单每家酒吧只配一行，固定金额与固定单价二选一且不小于 0");
            }
            out.add(new FirstSegmentRow(merchant, in.fixedAmountCent() != null
                    ? JdbcSettlementBillRepository.RULE_FIXED : JdbcSettlementBillRepository.RULE_PER_GUEST,
                    in.perGuestCent(), in.fixedAmountCent()));
        }
        return out;
    }

    private void notifyAffectedBars(String versionId, String projectId, LocalDate effectiveDate, LocalDate incentiveUntil) {
        // 全平台版本：所在项目已有不晚于本版生效（按生效营业日）的本项目版的酒吧，由本项目版管辖（effectiveVersionId 本项目版优先），
        //   本版对其不生效，不算受影响；本项目版晚于本版生效的，中间这段时间本版仍适用，照常通告；
        //   凭激励转正的本项目版不算（裁决第 12 条：激励一到期新售票就落回此前方案，没有本项目上一版即本版）。
        List<String> bars = jdbc.queryForList("""
                SELECT a.merchant_id FROM (
                    SELECT et.merchant_id FROM ticket_type_entitlements tte
                      JOIN ticket_types tt ON tt.id = tte.ticket_type_id AND tt.status = 'published' AND tt.bar_id IS NULL
                      JOIN entitlement_templates et ON et.id = tte.entitlement_template_id
                           AND et.archived_at IS NULL AND et.exclusive_merchant_id IS NULL
                      JOIN merchants m ON m.id = et.merchant_id AND m.merchant_type = 'bar'
                     WHERE et.entitlement_type IN ('entry', 'admission')
                       AND (:project_id::text IS NULL OR m.project_id = :project_id)
                    UNION
                    SELECT merchant_id FROM ticket_distribution_first_segment_rows WHERE version_id = :v) a
                 WHERE NOT EXISTS (
                    SELECT 1 FROM merchants m
                      JOIN ticket_distribution_scheme_versions me ON me.id = :v AND me.project_id IS NULL
                      JOIN ticket_distribution_scheme_versions pv ON pv.project_id = m.project_id AND pv.status = 'notified'
                     WHERE m.id = a.merchant_id
                       AND (pv.effective_at AT TIME ZONE :zone)::date <= (me.effective_at AT TIME ZONE :zone)::date
                       AND %s = 'infinity'::date)
                 ORDER BY 1
                """.formatted(incentiveExpiryDate("pv")),
                new MapSqlParameterSource().addValue("v", versionId).addValue("project_id", projectId)
                        .addValue("zone", LocalZone.ID.getId()), String.class);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("effective_date", effectiveDate.toString());
        params.put("version_id", versionId);
        String tiers = tierText(tierRows(versionId));
        Map<String, FirstSegmentRow> first = new LinkedHashMap<>();
        firstSegment(versionId).forEach(f -> first.put(f.merchantId(), f));
        // 裁决第 12 条：凭激励转正的版本，到期日与「到期未重新获批就改回此前方案」在转正当天的这条通告里一并写明。
        String incentiveText = incentiveUntil == null ? "" : "\n本版凭短期经营激励转正，激励到期日为 " + incentiveUntil.format(CN_DATE)
                + "；到期未重新获批的，自该日起新售出的票改按此前在用的方案。";
        for (String bar : bars) {
            // 18B 复核后裁决第 10 条：挂进商户专区＝送达（没有联系手机号的照挂）；只写本店的第一段，不写其他酒吧的（ENUM L52）。
            zoneNotices.post("mzn_" + Hashing.idempotencyCode(versionId + "|" + bar), bar, versionId, "通票分账方案变更通告",
                    "变更后的方案自 " + effectiveDate.format(CN_DATE) + " 起生效。\n参与家数档位表（第二段按本张票的第二段参与家数取行）：" + tiers
                            + "\n" + ownFirstSegmentText(first.get(bar)) + incentiveText, effectiveDate);
            // 短信只是提醒，走现有可关开关；发不发、成不成功都不影响送达与生效。
            String phone = recipients.resolvePhone(bar, null);
            if (phone != null) {
                notifications.notify("settlement", "distribution_scheme_notice", phone,
                        "sms.settlement.distribution_scheme_notice", params,
                        "tdsn_" + Hashing.idempotencyCode(versionId + "|" + bar));
            }
        }
        audit.recordSuccess("settlement.config.distribution_scheme.notice_sent", PERM, TABLE, versionId, null, null,
                Map.of("delivered", bars.size(), "effective_date", effectiveDate.toString()));
    }

    /** 档位表各行：「参与 N 家：第 1 名 x%、……」，规则对酒吧完全可见（ENUM L52）。 */
    private static String tierText(List<SchemeTier> tiers) {
        Map<Integer, List<String>> rows = new TreeMap<>();
        for (SchemeTier t : tiers) {
            rows.computeIfAbsent(t.participantCount(), k -> new ArrayList<>())
                    .add("第 " + t.rankNo() + " 名 " + BigDecimal.valueOf(t.shareBps(), 2).stripTrailingZeros().toPlainString() + "%");
        }
        StringBuilder text = new StringBuilder();
        rows.forEach((n, cells) -> text.append("\n参与 ").append(n).append(" 家：").append(String.join("、", cells)));
        return text.toString();
    }

    private static String ownFirstSegmentText(FirstSegmentRow own) {
        if (own == null) {
            return "本店不在第一段名单";
        }
        return "本店第一段：" + (JdbcSettlementBillRepository.RULE_FIXED.equals(own.ruleType()) ? "固定金额" : "固定单价")
                + "，每份票 " + yuan(own.amountCent()) + " 元";
    }

    private static Instant instant(Object value) {
        return value == null ? null : ((Timestamp) value).toInstant();
    }
}
