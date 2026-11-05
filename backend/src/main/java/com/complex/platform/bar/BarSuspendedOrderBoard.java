package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.security.ExportFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 T 段第 157 条 157-R7：挂起订单风险看板与审计导出。依据 BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和
 * 未结清数量。」；BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕（见平台治理地图）。」
 * <ul>
 *   <li>范围（照 142-R3 班次列表闸）：商户账号只看本酒吧；平台管理员看全部；其余按查看订单码 bar.order.view 可触及的项目。</li>
 *   <li>未结清＝已挂起待付款、取消审批中、坏账损耗处理中；账龄＝挂起时点到此刻（未结清）或到进入其余状态的时点（updated_at）。</li>
 *   <li>责任人（18T 复核修复④）：读时按本单未作废已出品明细逐项取其出品时刻本单开台会话的主责服务员（与赔偿登记同一查询口径；BSM:1110「服务员责任以商品
 *       出品时该桌绑定的主责服务员为准，后续换台、换班或改绑不追溯变更；出品时没有主责服务员的记为门店损耗」），多人全列，有明细取不到主责区间的列门店损耗，
 *       尚无已出品明细的留空。挂起记录里挂起时写入的 responsible_user_id 保留作挂起留痕（BSM:1103），看板与导出不用它。</li>
 *   <li>导出照审计导出：单次授权 audit.operation_log.export、导一次扣一次，只交带密码的 PDF，落导出记录与审计。</li>
 * </ul>
 */
@Service
public class BarSuspendedOrderBoard {

    static final String VIEW = "bar.order.view";
    static final String EXPORT = "audit.operation_log.export";
    static final String EXPORT_TYPE = "bar_suspended_orders";
    private static final String UNSETTLED = "('hanging_unpaid', 'cancel_pending_approval', 'bad_debt_processing')";
    private static final Map<String, String> STATUS_LABELS = Map.of("hanging_unpaid", "已挂起待付款", "settled", "已结清",
            "cancel_pending_approval", "取消审批中", "cancelled", "已取消", "pending_refund", "待退款",
            "bad_debt_processing", "坏账损耗处理中", "written_off_closed", "已核销结案");
    private static final String STORE_LOSS = "门店损耗";
    private static final List<String> EXPORT_COLUMNS = List.of("账单号", "酒吧", "桌台", "状态", "挂起金额（元）", "未收金额（元）",
            "挂起时间", "账龄", "责任人", "挂起人");
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(LocalZone.ID);
    private static final String ROWS = """
            SELECT s.order_id, b.bill_no, s.bar_id, m.merchant_name AS bar_name, s.table_id, t.table_name, s.status, s.amount_cent,
                   (SELECT COALESCE(sum(x.payable_amount_cent + x.tip_amount_cent - x.paid_amount_cent), 0) FROM bar_bills x
                     WHERE (x.order_id = s.order_id OR x.id = s.bill_id)
                       AND x.settlement_status NOT IN ('settled', 'merged', 'void')) AS unpaid_cent,
                   s.suspended_at,
                   floor(extract(epoch FROM (CASE WHEN s.status IN %s THEN now() ELSE s.updated_at END) - s.suspended_at) / 60)::bigint
                       AS age_minutes,
                   resp.users AS responsible_users, s.suspended_by
              FROM bar_order_suspensions s
              JOIN bar_bills b ON b.id = s.bill_id
              LEFT JOIN merchants m ON m.id = s.bar_id
              LEFT JOIN bar_tables t ON t.id = s.table_id
              LEFT JOIN LATERAL (
                   SELECT COALESCE(jsonb_agg(DISTINCT jsonb_build_object('user_id', d.user_id, 'username', u.username)), '[]'::jsonb)::text AS users
                     FROM bar_order_items i
                     LEFT JOIN LATERAL (SELECT r.user_id FROM bar_order_service_sources ss
                                          JOIN bar_service_responsibilities r ON r.session_id = ss.session_id
                                         WHERE ss.order_id = i.order_id AND r.started_at <= i.delivered_at
                                           AND (r.ended_at IS NULL OR r.ended_at > i.delivered_at)
                                         ORDER BY r.started_at DESC LIMIT 1) d ON TRUE
                     LEFT JOIN access_users u ON u.id = d.user_id
                    WHERE i.order_id = s.order_id AND i.delivery_status = 'delivered' AND i.voided IS NOT TRUE) resp ON TRUE
            """.formatted(UNSETTLED);

    /** 看板一行：账龄为分钟；责任人逐人列出（空列表＝尚无已出品明细）。 */
    public record SuspensionBoardRow(String orderId, String billNo, String barId, String barName, String tableId, String tableName, String status,
            long amountCent, long unpaidCent, Instant suspendedAt, long ageMinutes, List<Responsible> responsibleUsers, String suspendedBy) {
    }

    /** 出品时主责服务员：userId 为空＝门店损耗（出品时没有主责服务员）；username 为其账号。 */
    public record Responsible(String userId, String username) {
    }

    /** 未结清汇总（按范围与酒吧筛选，不受状态筛选与分页影响）：数量、未收金额合计、最长账龄（分钟）。 */
    public record SuspensionBoardSummary(long unsettledCount, long unsettledAmountCent, long oldestUnsettledAgeMinutes) {
    }

    /** 看板：汇总＋本页各行（键名用 orders，不叫 items，后台客户端才不会把汇总拍平丢掉）。 */
    public record SuspensionBoard(SuspensionBoardSummary summary, List<SuspensionBoardRow> orders, int page, int size, long total) {
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService authorization;
    private final AuditEventService audit;
    private final ExportFileService exportFiles;
    private final ObjectMapper json;

    public BarSuspendedOrderBoard(JdbcTemplate jdbc, AuthorizationService authorization, AuditEventService audit,
            ExportFileService exportFiles, ObjectMapper json) {
        this.jdbc = jdbc;
        this.authorization = authorization;
        this.audit = audit;
        this.exportFiles = exportFiles;
        this.json = json;
    }

    /** 看板读取；成功留痕 bar.order_suspension.viewed，失败 bar.order_suspension.view_failed。 */
    public SuspensionBoard board(String status, String barId, int page, int size) {
        try {
            int safePage = Math.max(1, page);
            int safeSize = Math.min(100, Math.max(1, size));
            Filter filter = filter(status, barId);
            List<Object> args = new ArrayList<>(filter.args());
            args.add(safeSize);
            args.add((safePage - 1) * safeSize);
            List<SuspensionBoardRow> rows = jdbc.query(ROWS + filter.where() + " ORDER BY s.suspended_at DESC, s.order_id LIMIT ? OFFSET ?", this::row,
                    args.toArray());
            Long total = jdbc.queryForObject("SELECT count(*) FROM bar_order_suspensions s" + filter.where(), Long.class,
                    filter.args().toArray());
            SuspensionBoard board = new SuspensionBoard(summary(filter), rows, safePage, safeSize, total == null ? 0 : total);
            audit.recordSuccess("bar.order_suspension.viewed", VIEW, "bar_order_suspension", "all", null, null,
                    Map.of("count", rows.size(), "total", board.total()));
            return board;
        } catch (RuntimeException error) {
            audit.recordFailure("bar.order_suspension.view_failed", VIEW, "bar_order_suspension", "all", null, null, codeOf(error));
            throw error;
        }
    }

    /** 导出：只交带密码的 PDF；成功留痕 bar.order_suspension.exported（business_id 导出文件编号），失败 bar.order_suspension.export_failed。 */
    public ExportFileService.ExportFile export(String status, String barId) {
        try {
            authorization.requirePermission(EXPORT);
            ExportFileService.Table table = table(status, barId);
            ExportFileService.ExportFile file = exportFiles.create(EXPORT_TYPE, table.title(), table.filters(), table.columns(), table.rows());
            audit.recordSuccess("bar.order_suspension.exported", EXPORT, "export_file", file.id(), null, null,
                    Map.of("row_count", table.rows().size()));
            return file;
        } catch (RuntimeException error) {
            audit.recordFailure("bar.order_suspension.export_failed", EXPORT, "export_file", null, null, null, codeOf(error));
            throw error;
        }
    }

    /** 导出内容（含 23-R4 可编辑版重取）：范围内全部行，标题带未结清数量与金额；导出记录 filters 记筛选与取数范围。 */
    ExportFileService.Table table(String status, String barId) {
        Filter filter = filter(status, barId);
        SuspensionBoardSummary summary = summary(filter);
        List<List<String>> rows = jdbc.query(ROWS + filter.where() + " ORDER BY s.suspended_at DESC, s.order_id", this::row,
                filter.args().toArray()).stream().map(BarSuspendedOrderBoard::exportRow).toList();
        return new ExportFileService.Table("挂起订单风险看板（未结清 " + summary.unsettledCount() + " 单，未收 "
                + yuan(summary.unsettledAmountCent()) + " 元）", filter.filters(), EXPORT_COLUMNS, rows);
    }

    /** 查询条件：scoped 只含范围与酒吧（汇总用，不受状态筛选影响），where 再加状态；filters 记进导出记录。 */
    private record Filter(String scoped, List<Object> scopedArgs, String where, List<Object> args, Map<String, Object> filters) {
    }

    /** 范围闸（照 142-R3 班次列表）＋酒吧、状态筛选。 */
    private Filter filter(String status, String barId) {
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status", status);
        filters.put("bar_id", barId);
        var actor = authorization.currentActor();
        Optional<String> merchant = authorization.currentMerchantIdQuietly();
        if (merchant.isPresent()) {
            where.add("s.merchant_id = ?");
            args.add(merchant.get());
            filters.put("merchant_id", merchant.get());
        } else if (!authorization.isPlatformAdmin(actor)) {
            TreeSet<String> projects = new TreeSet<>(authorization.projectReach(actor, VIEW).organizationIds());
            where.add(projects.isEmpty() ? "FALSE" : "s.project_id IN (" + String.join(", ", projects.stream().map(p -> "?").toList()) + ")");
            args.addAll(projects);
            filters.put("project_ids", projects);
        }
        if (barId != null && !barId.isBlank()) {
            where.add("s.bar_id = ?");
            args.add(barId);
        }
        String scoped = clause(where);
        List<Object> scopedArgs = List.copyOf(args);
        if (status != null && !status.isBlank()) {
            where.add("s.status = ?");
            args.add(status);
        }
        return new Filter(scoped, scopedArgs, clause(where), args, filters);
    }

    private static String clause(List<String> where) {
        return where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
    }

    private SuspensionBoardSummary summary(Filter filter) {
        return jdbc.queryForObject("SELECT count(*), COALESCE(sum(r.unpaid_cent), 0), COALESCE(max(r.age_minutes), 0) FROM (" + ROWS
                + filter.scoped() + ") r WHERE r.status IN " + UNSETTLED, (rs, n) -> new SuspensionBoardSummary(rs.getLong(1), rs.getLong(2), rs.getLong(3)),
                filter.scopedArgs().toArray());
    }

    private SuspensionBoardRow row(ResultSet rs, int n) throws SQLException {
        return new SuspensionBoardRow(rs.getString("order_id"), rs.getString("bill_no"), rs.getString("bar_id"), rs.getString("bar_name"),
                rs.getString("table_id"), rs.getString("table_name"), rs.getString("status"), rs.getLong("amount_cent"),
                rs.getLong("unpaid_cent"), rs.getTimestamp("suspended_at").toInstant(), rs.getLong("age_minutes"),
                responsibles(rs.getString("responsible_users")), rs.getString("suspended_by"));
    }

    private List<Responsible> responsibles(String text) {
        try {
            return json.readValue(text, new TypeReference<List<Map<String, String>>>() { }).stream()
                    .map(entry -> new Responsible(entry.get("user_id"), entry.get("username"))).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> exportRow(SuspensionBoardRow row) {
        return Arrays.asList(row.billNo(), row.barName() == null ? row.barId() : row.barName(),
                row.tableName() == null ? row.tableId() : row.tableName(), STATUS_LABELS.getOrDefault(row.status(), row.status()),
                yuan(row.amountCent()), yuan(row.unpaidCent()), EXPORT_TIME.format(row.suspendedAt()), age(row.ageMinutes()),
                row.responsibleUsers().stream().map(r -> r.userId() == null ? STORE_LOSS : (r.username() == null ? r.userId() : r.username()))
                        .collect(Collectors.joining("、")), row.suspendedBy());
    }

    private static String yuan(long cent) {
        return java.math.BigDecimal.valueOf(cent, 2).toPlainString();
    }

    private static String age(long minutes) {
        return minutes >= 60 ? (minutes / 60) + " 小时 " + (minutes % 60) + " 分" : minutes + " 分";
    }

    private static String codeOf(RuntimeException error) {
        return error instanceof ApiException api ? api.code() : error.getClass().getSimpleName();
    }
}
