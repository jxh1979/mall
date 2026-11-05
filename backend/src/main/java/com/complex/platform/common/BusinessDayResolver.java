package com.complex.platform.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 营业日解析（ENUM:187「营业日从当日上午 10:00 起算，至次日上午 10:00 止……起止时点由后台配置（安全阈值，按每个项目、
 * 每家酒吧可配……出厂默认起点为上午 10:00、闭店为次日 07:00……）」；BSM:1682；DEC-BIZDAY-001）。
 *
 * <p>传了酒吧（barId 非空；酒吧＝业态为酒吧的商户，V267）按该酒吧已批准且已生效的版本（bar_business_day_configs）；没传酒吧按综合体的版本
 * （amusement_business_day_configs，即游乐已在用的那一套，BSM:1682「按综合体分别配置」）；查不到版本或对象为空一律
 * 出厂起点 10:00。酒吧没有自己的版本时也是出厂 10:00，不借所在综合体的版本。时区取综合体已批准版本的所在地时区，
 * 没有版本取 {@link LocalZone}。版本只从 effective_at 起生效（营业日配置的变更只影响未来），某营业日跨版本切换时
 * 窗口取各段之并（切换处首尾相接）；起点调晚、生效时点又落在当日新旧起点之间的，起用顺延到当日新起点（一个时刻只属于
 * 一个营业日）。闭店时点（07:00 起至起点前为非营业时段）仍归正在进行的营业日；闭店时刻另供日结五类最终标记判「闭店之后」
 * （{@link #closeAt}，第十八批 R 段 143-R2）。
 *
 * <p>游乐核销与场次不经本类：无已批准版本时拒绝，见 AmusementBusinessDayService。
 */
@Component
// 文件版本：V0.03
public class BusinessDayResolver {

    /** 出厂起点：上午 10:00。 */
    public static final int FACTORY_CUTOFF_HOUR = 10;
    /** 出厂闭店：次日 07:00（BSM:1682「出厂默认起点为上午 10:00、闭店为次日 07:00」）。 */
    public static final int FACTORY_CLOSE_HOUR = 7;
    static final long CACHE_TTL_MS = 60_000;

    private record Segment(Instant from, int cutoffHour, int closeHour, ZoneId zone) {}
    private record Timeline(List<Segment> segments, long loadedAt) {}

    private static final List<Segment> FACTORY = List.of(new Segment(null, FACTORY_CUTOFF_HOUR, FACTORY_CLOSE_HOUR, LocalZone.ID));

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate;
    private final Map<String, Timeline> cache = new ConcurrentHashMap<>();

    /** 无库构造（单测）：一律出厂起点。 */
    public BusinessDayResolver() {
        this(null);
    }

    @Autowired
    public BusinessDayResolver(ObjectProvider<NamedParameterJdbcTemplate> jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 时刻 at 所属的营业日。 */
    public LocalDate businessDate(String projectId, String barId, Instant at) {
        Segment segment = segmentAt(segments(projectId, barId), at);
        LocalDate date = at.atZone(segment.zone()).toLocalDate();
        return at.isBefore(cut(segment, date)) ? date.minusDays(1) : date;
    }

    /** 营业日起始时刻（含）。 */
    public Instant start(String projectId, String barId, LocalDate businessDate) {
        return window(segments(projectId, barId), businessDate)[0];
    }

    /** 营业日结束时刻（不含）。 */
    public Instant end(String projectId, String barId, LocalDate businessDate) {
        return window(segments(projectId, barId), businessDate)[1];
    }

    /**
     * 第十八批 R 段 143-R2（ENUM:190「由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」；BSM:1682「出厂默认起点为上午 10:00、
     * 闭店为次日 07:00」）：营业日的闭店时刻＝营业日次日的闭店整点，闭店时点与时区取该营业日起点所在的版本（酒吧按该酒吧，否则按综合体）。
     */
    public java.time.ZonedDateTime closeAt(String projectId, String barId, LocalDate businessDate) {
        List<Segment> segments = segments(projectId, barId);
        Segment segment = segmentAt(segments, window(segments, businessDate)[0]);
        return businessDate.plusDays(1).atTime(segment.closeHour(), 0).atZone(segment.zone());
    }

    /** 酒吧＝业态为酒吧的商户（V267），酒吧 id 即商户 id：该商户是酒吧时返回它，否则 null（按综合体营业日）。 */
    public String barOfMerchant(String merchantId) {
        NamedParameterJdbcTemplate template = template();
        if (template == null || merchantId == null) {
            return null;
        }
        return template.queryForList("SELECT id FROM merchants WHERE id = :merchant_id AND merchant_type = 'bar'",
                Map.of("merchant_id", merchantId), String.class).stream().findFirst().orElse(null);
    }

    /**
     * 按商户落账时刻 at 的营业日：商户是酒吧按该酒吧，否则按 projectId（未给取商户所属项目）。
     * BSM:1682「按综合体分别配置……酒吧仍按每家酒吧配置」。
     */
    public LocalDate merchantBusinessDate(String projectId, String merchantId, Instant at) {
        return businessDate(projectId != null ? projectId : projectOfMerchant(merchantId), barOfMerchant(merchantId), at);
    }

    /**
     * 酒吧账单所属营业日（ENUM:187「销售提成月份，一律以开台或下单时点所属的营业日为准」）：账单所在订单落单时锁定了开台会话的取开台营业日
     * （bar_order_service_sources.business_date），否则按该酒吧营业日解析下单时刻。查不到账单为 null。
     */
    public LocalDate barBillBusinessDate(String billId) {
        NamedParameterJdbcTemplate template = template();
        if (template == null) {
            return null;
        }
        return template.query("SELECT " + BAR_BILL_DAY_COLUMNS + " FROM bar_bills b" + BAR_BILL_DAY_JOINS + " WHERE b.id = :bill_id",
                Map.of("bill_id", billId), this::barBillDay).stream().findFirst().orElse(null);
    }

    /**
     * 销售提成归属 month 的酒吧提成计提 id（口径同 {@link #barBillBusinessDate}）；salesStaffId 为空＝不限员工。
     * SQL 先按开台营业日或下单自然日前后各放一天粗筛，再逐笔按营业日定月。
     */
    public List<String> barCommissionIdsInMonth(java.time.YearMonth month, String salesStaffId) {
        NamedParameterJdbcTemplate template = template();
        if (template == null) {
            return List.of();
        }
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("staff", salesStaffId, java.sql.Types.VARCHAR)
                .addValue("zone", LocalZone.ID.getId())
                .addValue("from", java.sql.Date.valueOf(month.atDay(1).minusDays(1)))
                .addValue("to", java.sql.Date.valueOf(month.atEndOfMonth().plusDays(1)));
        List<String> ids = new ArrayList<>();
        template.query("SELECT c.id AS commission_id, " + BAR_BILL_DAY_COLUMNS
                + " FROM bar_commission_calculations c JOIN bar_bills b ON b.id = c.bill_id" + BAR_BILL_DAY_JOINS + """
                 WHERE (CAST(:staff AS VARCHAR) IS NULL OR c.sales_staff_id = :staff)
                   AND COALESCE(src.business_date, (o.created_at AT TIME ZONE :zone)::date) BETWEEN :from AND :to
                 ORDER BY c.id
                """, params, rs -> {
                    if (java.time.YearMonth.from(barBillDay(rs, 0)).equals(month)) {
                        ids.add(rs.getString("commission_id"));
                    }
                });
        return ids;
    }

    private static final String BAR_BILL_DAY_COLUMNS = "src.business_date AS session_business_date, b.bar_id, o.created_at AS ordered_at";
    private static final String BAR_BILL_DAY_JOINS = """
             JOIN bar_orders o ON o.id = b.order_id
             LEFT JOIN bar_order_service_sources src ON src.order_id = b.order_id
            """;

    private LocalDate barBillDay(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        java.sql.Date session = rs.getDate("session_business_date");
        return session != null ? session.toLocalDate() : businessDate(null, rs.getString("bar_id"), rs.getTimestamp("ordered_at").toInstant());
    }

    /** 看板单综合体口径的综合体：唯一启用的项目；没有或不止一个时 null（出厂营业日）。 */
    public String soleProject() {
        NamedParameterJdbcTemplate template = template();
        if (template == null) {
            return null;
        }
        List<String> ids = template.queryForList("SELECT id FROM projects WHERE status = 'active' ORDER BY id LIMIT 2",
                Map.of(), String.class);
        return ids.size() == 1 ? ids.getFirst() : null;
    }

    /** 商户（含酒吧）所属项目（merchants.project_id），查不到为 null。 */
    public String projectOfMerchant(String merchantId) {
        NamedParameterJdbcTemplate template = template();
        if (template == null || merchantId == null) {
            return null;
        }
        return template.queryForList("SELECT project_id FROM merchants WHERE id = :merchant_id AND project_id IS NOT NULL",
                Map.of("merchant_id", merchantId), String.class).stream().findFirst().orElse(null);
    }

    /**
     * 该营业日窗口与出厂窗口不同的酒吧及其窗口（有已批准版本，或所在综合体的时区不是 {@link LocalZone}）；
     * 其余酒吧（含没有主数据的历史 bar_id）一律出厂窗口。供跨酒吧汇总逐家取数。
     */
    public Map<String, Instant[]> barWindows(LocalDate businessDate) {
        NamedParameterJdbcTemplate template = template();
        if (template == null) {
            return Map.of();
        }
        Instant[] factory = window(FACTORY, businessDate);
        Map<String, Instant[]> windows = new LinkedHashMap<>();
        for (String bar : template.queryForList("""
                SELECT c.bar_id FROM bar_business_day_configs c WHERE c.status = 'approved'
                UNION
                SELECT m.id FROM merchants m
                 WHERE m.merchant_type = 'bar' AND EXISTS (SELECT 1 FROM amusement_business_day_configs c
                        WHERE c.project_id = m.project_id AND c.status = 'approved' AND c.zone_id <> :zone)
                 ORDER BY 1
                """, Map.of("zone", LocalZone.ID.getId()), String.class)) {
            Instant[] window = window(segments(null, bar), businessDate);
            if (!Arrays.equals(window, factory)) {
                windows.put(bar, window);
            }
        }
        return windows;
    }

    /** 新版本批准后立即可见（不等缓存过期）。 */
    public void invalidate() {
        cache.clear();
    }

    private static Instant cut(Segment segment, LocalDate date) {
        return date.atTime(segment.cutoffHour(), 0).atZone(segment.zone()).toInstant();
    }

    private static Segment segmentAt(List<Segment> segments, Instant at) {
        Segment found = segments.getFirst();
        for (Segment segment : segments) {
            if (segment.from() == null || !at.isBefore(segment.from())) {
                found = segment;
            }
        }
        return found;
    }

    private static Instant[] window(List<Segment> segments, LocalDate date) {
        Instant start = null;
        Instant end = null;
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            Instant from = cut(segment, date);
            Instant to = cut(segment, date.plusDays(1));
            Instant next = i + 1 < segments.size() ? segments.get(i + 1).from() : null;
            if (segment.from() != null && from.isBefore(segment.from())) {
                from = segment.from();
            }
            if (next != null && next.isBefore(to)) {
                to = next;
            }
            if (from.isBefore(to)) {
                start = start == null || from.isBefore(start) ? from : start;
                end = end == null || to.isAfter(end) ? to : end;
            }
        }
        if (start == null) {
            Segment segment = segmentAt(segments, cut(segments.getFirst(), date));
            return new Instant[] {cut(segment, date), cut(segment, date.plusDays(1))};
        }
        return new Instant[] {start, end};
    }

    private List<Segment> segments(String projectId, String barId) {
        NamedParameterJdbcTemplate template = template();
        String key = barId != null ? "bar:" + barId : projectId != null ? "project:" + projectId : null;
        if (template == null || key == null) {
            return FACTORY;
        }
        long now = System.currentTimeMillis();
        Timeline cached = cache.get(key);
        if (cached != null && now - cached.loadedAt() < CACHE_TTL_MS) {
            return cached.segments();
        }
        List<Segment> segments = new ArrayList<>();
        if (barId != null) {
            ZoneId zone = template.queryForList("""
                    SELECT c.zone_id FROM amusement_business_day_configs c JOIN merchants m ON m.project_id = c.project_id
                     WHERE m.id = :id AND c.status = 'approved' AND c.effective_at <= now()
                     ORDER BY c.effective_at DESC LIMIT 1
                    """, Map.of("id", barId), String.class).stream().findFirst().map(ZoneId::of).orElse(LocalZone.ID);
            segments.add(new Segment(null, FACTORY_CUTOFF_HOUR, FACTORY_CLOSE_HOUR, zone));
            segments.addAll(template.query("""
                    SELECT effective_at, cutoff_hour, close_hour FROM bar_business_day_configs
                     WHERE bar_id = :id AND status = 'approved' ORDER BY effective_at
                    """, Map.of("id", barId), (rs, n) -> new Segment(rs.getTimestamp(1).toInstant(), rs.getInt(2), rs.getInt(3), zone)));
        } else {
            segments.add(FACTORY.getFirst());
            segments.addAll(template.query("""
                    SELECT effective_at, cutoff_hour, zone_id, close_hour FROM amusement_business_day_configs
                     WHERE project_id = :id AND status = 'approved' ORDER BY effective_at
                    """, Map.of("id", projectId), (rs, n) -> new Segment(rs.getTimestamp(1).toInstant(), rs.getInt(2),
                    rs.getInt(4), ZoneId.of(rs.getString(3)))));
        }
        List<Segment> loaded = joined(segments);
        cache.put(key, new Timeline(loaded, now));
        return loaded;
    }

    /** 起点调晚、生效时点落在当日（旧起点, 新起点）之间的段，起用顺延到当日新起点，前后营业日首尾相接、窗口不重叠。 */
    private static List<Segment> joined(List<Segment> segments) {
        List<Segment> out = new ArrayList<>();
        for (Segment segment : segments) {
            Instant from = segment.from();
            if (from != null && !out.isEmpty()) {
                LocalDate day = from.atZone(segment.zone()).toLocalDate();
                Instant newStart = cut(segment, day);
                if (from.isAfter(cut(out.getLast(), day)) && from.isBefore(newStart)) {
                    from = newStart;
                }
            }
            out.add(new Segment(from, segment.cutoffHour(), segment.closeHour(), segment.zone()));
        }
        return List.copyOf(out);
    }

    private NamedParameterJdbcTemplate template() {
        return jdbcTemplate == null ? null : jdbcTemplate.getIfAvailable();
    }
}
