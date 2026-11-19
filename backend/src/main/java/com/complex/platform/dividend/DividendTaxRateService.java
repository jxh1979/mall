package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dividend.dto.DividendTaxRateRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.EntityTaxService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AX 段第 271 条 271-R1 全平台统一分红代扣个税税率版本（BSM:1776「其分得的款项按「股息红利」计税，由发放的经营主体代扣个人所得税（安全阈值，
 * 全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限），发放时在单据上写清代扣了多少」「税率以参数形式
 * 配置，不在业务页面硬编码」；BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」；
 * DEC-R8-82「调整须走最高一级审批与三审留痕」）。
 * <ul>
 *   <li>平台财务在持股分红页提交新版本：税率、上下限（下限 ≤ 税率 ≤ 上限）、生效起始日期与理由必填；同一生效起始日已有待审或已生效版本 409
 *       （照 233-R2 EntityTaxService.submitRate，不设「生效日须晚于今天」与待审互斥）。</li>
 *   <li>审批照 233-R2：财务、税务、法务三条真实部门起点＋复核动作 finance.tax_rule.adjust（终点最高权限确认，恒进复核，最高权限本人提交同样进审批），
 *       结果由 {@link #decided} 回写，批准落已生效、驳回落已驳回。</li>
 *   <li>出单取出单当日营业日已生效的版本（{@link #inForce}），主理人与其他个人股东同用，版本号固化在计算单上；出厂版本自始有效、排最后。</li>
 * </ul>
 * 写路径走 {@link AuditedOperationExecutor#executeWrite}，范围闸放在幂等之前；直接用 JdbcTemplate（照 DividendCloseoutService）。
 */
@Service
public class DividendTaxRateService {

    static final String OBJECT = "dividend_tax_rate";
    private static final String ACTION = "finance.tax_rule.adjust";
    private static final String SELECT = "SELECT id, rate_bps, min_bps, max_bps, effective_from, status, reason, review_id, created_by, created_at"
            + " FROM dividend_tax_rate_versions";

    /** 一个税率版本。effectiveFrom 为空＝出厂版本自始有效；status：pending_review 审批中、approved 已生效、rejected 已驳回。 */
    public record Version(String id, int rateBps, Integer minBps, Integer maxBps, LocalDate effectiveFrom, String status, String reason,
                          String reviewId, String createdBy, Instant createdAt) {
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final AuditedOperationExecutor operations;
    private final EntityTaxService entityTaxes;

    public DividendTaxRateService(JdbcTemplate jdbc, AuthorizationService auth, AuditedOperationExecutor operations, EntityTaxService entityTaxes) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.operations = operations;
        this.entityTaxes = entityTaxes;
    }

    /** 全部版本（含审批中与已驳回），生效起始日新的在前，出厂版本在末。平台口径（商户账号拒）。 */
    public List<Version> list() {
        auth.requirePermission(DividendService.VIEW_PERM);
        auth.requirePlatformScope();
        return operations.readSingle(
                AuditedOperation.of("dividend.tax_rates_listed", "dividend.tax_rates_list_failed", DividendService.VIEW_PERM, OBJECT),
                () -> jdbc.query(SELECT + " ORDER BY effective_from DESC NULLS LAST, created_at DESC, id", this::row),
                rows -> "all");
    }

    /** 提交新版本并进财务、税务、法务三审与最高确认。 */
    @Transactional
    public Version submit(DividendTaxRateRequest request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        if (request == null) {
            throw invalid("请求内容不能为空");
        }
        return operations.executeWrite("submitDividendTaxRate", request.idempotencyKey(), request, Version.class,
                AuditedOperation.of("dividend.tax_rate_submitted", "dividend.tax_rate_submit_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    Integer rate = request.rateBps();
                    Integer min = request.minBps();
                    Integer max = request.maxBps();
                    if (rate == null || min == null || max == null) {
                        throw invalid("代扣税率须同时填写税率与上下限");
                    }
                    if (min < 0 || max > 10000 || min > rate || rate > max) {
                        throw invalid("代扣税率须在 0%～100% 之间，且下限 ≤ 税率 ≤ 上限");
                    }
                    LocalDate from = request.effectiveFrom();
                    if (from == null) {
                        throw invalid("须填写生效起始日期");
                    }
                    String reason = requireText(request.reason(), "reason");
                    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { }, "dividend-tax-rate");
                    if (jdbc.queryForObject("SELECT count(*) FROM dividend_tax_rate_versions WHERE effective_from = ?"
                            + " AND status IN ('pending_review', 'approved')", Integer.class, from) > 0) {
                        throw ApiException.conflict("DIVIDEND_TAX_RATE_VERSION_CONFLICT",
                                "这一生效起始日已有待审批或已生效的分红代扣税率版本，驳回后才能重新提交");
                    }
                    Version before = inForce(from);
                    String id = "dtrv_" + UUID.randomUUID().toString().replace("-", "");
                    String summary = "分红代扣个税（股息红利，全平台统一）：原生效 " + rateText(before.rateBps(), before.minBps(), before.maxBps())
                            + (before.effectiveFrom() == null ? "（出厂，自始有效）" : "（" + before.effectiveFrom() + " 起）")
                            + " → 新 " + rateText(rate, min, max) + "，自 " + from + " 起生效（此前出的单不变）";
                    var review = entityTaxes.submitReview(request.idempotencyKey(), List.of(), request.parallelLines(), OBJECT, id, reason, summary);
                    jdbc.update("INSERT INTO dividend_tax_rate_versions (id, rate_bps, min_bps, max_bps, effective_from, status, reason, review_id,"
                                    + " created_by) VALUES (?, ?, ?, ?, ?, 'pending_review', ?, ?, ?)",
                            id, rate, min, max, from, reason, review.reviewId(), auth.currentActor().id());
                    return jdbc.query(SELECT + " WHERE id = ?", this::row, id).getFirst();
                },
                saved -> ((Version) saved).id(), Map.of());
    }

    /** 复核终态回写：只认本动作与对象、仍待审批且复核单号相符、复核表里确有该终态的行（照 EntityTaxService#decided）。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT.equals(decision.objectType()) || !ACTION.equals(decision.actionType())
                || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        jdbc.update("UPDATE dividend_tax_rate_versions SET status = ? WHERE id = ? AND status = 'pending_review' AND review_id = ?"
                        + " AND EXISTS (SELECT 1 FROM supervisor_review_decisions WHERE review_id = ? AND action_type = ? AND object_type = ?"
                        + " AND object_id = ? AND review_status = ?)",
                decision.reviewStatus(), decision.objectId(), decision.reviewId(), decision.reviewId(), ACTION, OBJECT, decision.objectId(),
                decision.reviewStatus());
    }

    /** 某日已生效的版本：生效起始日不晚于该日的最新已批准版本，出厂版本排最后。不判登录人（月末自动预览无请求上下文）。 */
    public Version inForce(LocalDate date) {
        return jdbc.query(SELECT + " WHERE status = 'approved' AND (effective_from IS NULL OR effective_from <= ?)"
                        + " ORDER BY effective_from DESC NULLS LAST LIMIT 1", this::row, date).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少分红代扣税率出厂版本（V658）"));
    }

    private Version row(ResultSet rs, int n) throws SQLException {
        return new Version(rs.getString("id"), rs.getInt("rate_bps"), rs.getObject("min_bps", Integer.class), rs.getObject("max_bps", Integer.class),
                rs.getObject("effective_from", LocalDate.class), rs.getString("status"), rs.getString("reason"), rs.getString("review_id"),
                rs.getString("created_by"), nullableInstant(rs, "created_at"));
    }

    private static String rateText(int rate, Integer min, Integer max) {
        return percent(rate) + (min == null ? "（未配上下限）" : "（" + percent(min) + "～" + percent(max) + "）");
    }

    private static String percent(int bps) {
        return BigDecimal.valueOf(bps, 2).stripTrailingZeros().toPlainString() + "%";
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }
}
