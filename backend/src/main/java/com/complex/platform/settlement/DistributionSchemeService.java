package com.complex.platform.settlement;

// 文件版本：V0.01

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
 */
@Service
public class DistributionSchemeService {

    private static final String PERM = "settlement.config.manage";
    private static final String TABLE = "ticket_distribution_scheme_versions";
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_NOTIFIED = "notified";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditEventService audit;
    private final NotificationService notifications;
    private final MerchantFeeNotificationRecipientResolver recipients;
    private final BusinessDayResolver businessDays;
    private final MerchantZoneNoticeService zoneNotices;

    public DistributionSchemeService(NamedParameterJdbcTemplate jdbc, AuditEventService audit,
                                     NotificationService notifications, MerchantFeeNotificationRecipientResolver recipients,
                                     BusinessDayResolver businessDays, MerchantZoneNoticeService zoneNotices) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.notifications = notifications;
        this.recipients = recipients;
        this.businessDays = businessDays;
        this.zoneNotices = zoneNotices;
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

    /** state：draft 草案 / pending 已通告待生效 / effective 现行 / superseded 已被后续版本取代。 */
    public record SchemeVersion(String id, String projectId, String status, String state, String draftedBy, String approvedBy,
                                Instant notifiedAt, Instant effectiveAt, List<SchemeTier> tiers,
                                List<FirstSegmentRow> firstSegment) {
    }

    public record NoticePeriod(int periodValue, String periodUnit, Integer minValue, Integer maxValue) {
    }

    /** 「补配该家数档位行」待办：某项目缺某家数行而暂挂的份数与第二段余额合计。 */
    public record MissingRow(String projectId, int participantCount, int units, long amountCent) {
    }

    public record SchemeOverview(NoticePeriod noticePeriod, List<SchemeVersion> versions, List<MissingRow> missingRows) {
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
        NoticePeriod period = noticePeriod();
        String scopeProject = (String) row.get("project_id");
        LocalDate today = businessDays.businessDate(scopeProject, null, Instant.now());
        LocalDate effectiveDate = "day".equals(period.periodUnit())
                ? today.plusDays(period.periodValue()) : today.plusMonths(period.periodValue());
        jdbc.update("""
                UPDATE ticket_distribution_scheme_versions
                   SET status = 'notified', approved_by = :actor, notified_at = now(), effective_at = :effective_at
                 WHERE id = :id AND status = 'draft'
                """, new MapSqlParameterSource().addValue("id", versionId).addValue("actor", actorId)
                .addValue("effective_at", Timestamp.from(businessDays.start(scopeProject, null, effectiveDate))));
        audit.recordSuccess("settlement.config.draft.activated", PERM, TABLE, versionId, null, null,
                Map.of("config_group", TABLE, "effective_date", effectiveDate.toString()));
        notifyAffectedBars(versionId, (String) row.get("project_id"), effectiveDate);
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

    // ================= 读取 =================

    /** 各版本（状态、通告日、生效日、档位行与第一段名单）、通告期，以及「补配该家数档位行」待办。 */
    public SchemeOverview overview() {
        Instant now = Instant.now();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, project_id, status, drafted_by, approved_by, notified_at, effective_at
                  FROM ticket_distribution_scheme_versions ORDER BY project_id NULLS FIRST, created_at DESC, id
                """, new MapSqlParameterSource());
        // 各范围现行版：已通告且已到生效时刻的最晚一版。
        Map<String, Instant> currentAt = new LinkedHashMap<>();
        Map<String, String> currentId = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Instant at = instant(r.get("effective_at"));
            String scope = String.valueOf(r.get("project_id"));
            if (STATUS_NOTIFIED.equals(r.get("status")) && !at.isAfter(now)
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
        return new SchemeOverview(noticePeriod(), versions, missing);
    }

    /**
     * 时刻 at 售出的票所用方案版本：按该项目的售票营业日（ENUM L187）与各版生效营业日比较——本项目有已生效版本取本项目最晚一版，
     * 否则取全平台最晚一版（出厂版恒在）。全平台版本在各项目按各自营业日切换。
     */
    public String effectiveVersionId(String projectId, Instant at) {
        return jdbc.queryForList("""
                SELECT id FROM ticket_distribution_scheme_versions
                 WHERE status = 'notified' AND (effective_at AT TIME ZONE :zone)::date <= :sale_day
                   AND (project_id IS NULL OR project_id = :project_id)
                 ORDER BY (project_id IS NOT NULL) DESC, effective_at DESC, notified_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("zone", LocalZone.ID.getId())
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

    private void notifyAffectedBars(String versionId, String projectId, LocalDate effectiveDate) {
        // 全平台版本：所在项目已有不晚于本版生效（按生效营业日）的本项目版的酒吧，由本项目版管辖（effectiveVersionId 本项目版优先），
        //   本版对其不生效，不算受影响；本项目版晚于本版生效的，中间这段时间本版仍适用，照常通告。
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
                       AND (pv.effective_at AT TIME ZONE :zone)::date <= (me.effective_at AT TIME ZONE :zone)::date)
                 ORDER BY 1
                """, new MapSqlParameterSource().addValue("v", versionId).addValue("project_id", projectId)
                        .addValue("zone", LocalZone.ID.getId()), String.class);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("effective_date", effectiveDate.toString());
        params.put("version_id", versionId);
        String tiers = tierText(tierRows(versionId));
        Map<String, FirstSegmentRow> first = new LinkedHashMap<>();
        firstSegment(versionId).forEach(f -> first.put(f.merchantId(), f));
        for (String bar : bars) {
            // 18B 复核后裁决第 10 条：挂进商户专区＝送达（没有联系手机号的照挂）；只写本店的第一段，不写其他酒吧的（ENUM L52）。
            zoneNotices.post("mzn_" + Hashing.idempotencyCode(versionId + "|" + bar), bar, versionId, "通票分账方案变更通告",
                    "变更后的方案自 " + effectiveDate.format(CN_DATE) + " 起生效。\n参与家数档位表（第二段按本张票的第二段参与家数取行）：" + tiers
                            + "\n" + ownFirstSegmentText(first.get(bar)), effectiveDate);
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
                + "，每份票 " + BigDecimal.valueOf(own.amountCent(), 2).toPlainString() + " 元";
    }

    private static Instant instant(Object value) {
        return value == null ? null : ((Timestamp) value).toInstant();
    }
}
