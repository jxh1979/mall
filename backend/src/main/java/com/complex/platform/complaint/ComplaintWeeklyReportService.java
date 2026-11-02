package com.complex.platform.complaint;

// 文件版本：V0.01

import com.complex.platform.common.LocalZone;
import com.complex.platform.complaint.ComplaintRecipients.Recipient;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.CustomerComplaintService;
import com.complex.platform.governance.PlatformRepository;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 M 段第 111 条 111-R4 客诉周报（BSM:1385「系统每周自动生成客诉周报，列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人
 * 与店长，各公司的报送该公司最高负责人与平台最高负责人」「投诉分为商户投诉和消费客人投诉」；DEC-R4-2026-08-04-12「每周自动出客诉周报」）。
 * 第十八批裁决 28：标了门店的客人投诉，加上该店自己在商户专区提的投诉，进该店周报；全部客诉按所在项目进各公司周报。
 * 收件人按「接收客诉周报」码取（{@link ComplaintRecipients}）：商户账号收本店的，平台员工收该码可触及项目的；每人每周一条员工系统通知，重跑只覆盖不重发。
 */
@Service
public class ComplaintWeeklyReportService {

    static final String RECEIVE = "complaint.weekly_report.receive";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");
    /** ENUM:264 商户服务工单状态七态中文。 */
    private static final Map<String, String> MERCHANT_STATUS_LABELS = Map.of("created", "已提交", "assigned", "已派单", "processing", "处理中",
            "pending_merchant", "待商户确认", "resolved", "已处理待关闭", "closed", "已关闭", "cancelled", "已取消");

    private record Row(String chain, String status, String projectId, String store, boolean loopConfirmed) {
    }

    /** 当周一笔客诉：是否消费客人投诉（物业工单链）、状态中文、所在项目、涉及门店、是否已确认闭环。 */
    private record Complaint(boolean customer, String statusLabel, List<String> projects, String store, boolean loopConfirmed) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ComplaintRecipients recipients;
    private final PlatformRepository platformRepository;
    private final AuditEventService auditEventService;

    public ComplaintWeeklyReportService(NamedParameterJdbcTemplate jdbc, ComplaintRecipients recipients, PlatformRepository platformRepository,
            AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.recipients = recipients;
        this.platformRepository = platformRepository;
        this.auditEventService = auditEventService;
    }

    /** 周＝北京时间上周一 00:00 到本周一 00:00（now 所在周的前一周），按提出时间取客诉；返回发出的通知数。 */
    @Transactional
    public int sendWeeklyReports(Instant now) {
        LocalDate thisMonday = now.atZone(LocalZone.ID).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekStart = thisMonday.minusWeeks(1);
        List<Complaint> complaints = load(weekStart.atStartOfDay(LocalZone.ID).toInstant(), thisMonday.atStartOfDay(LocalZone.ID).toInstant());
        String title = "客诉周报（" + DAY.format(weekStart) + "～" + DAY.format(thisMonday.minusDays(1)) + "）";
        String noticePrefix = "notice_cwr_" + weekStart.format(DateTimeFormatter.BASIC_ISO_DATE) + "_";
        int sent = 0;
        for (Recipient recipient : recipients.holders(RECEIVE)) {
            List<Complaint> mine = complaints.stream().filter(complaint -> recipient.merchantId() != null
                    ? recipient.merchantId().equals(complaint.store())
                    : complaint.projects().stream().anyMatch(recipient.reach()::covers)).toList();
            recipients.notify(recipient, noticePrefix + recipient.profileId(), title, body(mine), now);
            sent++;
        }
        auditEventService.recordSuccess("complaint.weekly_report_sent", "not_applicable", "complaint_weekly_report", weekStart.toString(),
                UUID.randomUUID().toString(), null, Map.of("week_start", weekStart.toString(), "recipients", sent, "complaints", complaints.size()));
        return sent;
    }

    /** 物业工单链的投诉记「消费客人投诉」、门店取客诉跟进的涉及门店；商户服务工单链的投诉记「商户投诉」、门店即提单商户、项目取其经营项目。 */
    private List<Complaint> load(Instant from, Instant to) {
        List<Row> rows = jdbc.query("""
                SELECT 'facility' AS chain, w.status, w.project_id, f.store_merchant_id AS store, f.loop_confirmed_at
                  FROM facility_work_orders w
                  LEFT JOIN complaint_followups f ON f.chain = 'facility' AND f.work_order_id = w.id
                 WHERE w.work_order_type = 'complaint' AND w.created_at >= :from AND w.created_at < :to
                UNION ALL
                SELECT 'merchant_service', m.status, NULL, m.merchant_id, f.loop_confirmed_at
                  FROM merchant_service_work_orders m
                  LEFT JOIN complaint_followups f ON f.chain = 'merchant_service' AND f.work_order_id = m.id
                 WHERE m.work_order_type = 'complaint' AND m.delete_status <> 'approved' AND m.created_at >= :from AND m.created_at < :to
                """, new MapSqlParameterSource("from", Timestamp.from(from)).addValue("to", Timestamp.from(to)),
                (rs, n) -> new Row(rs.getString("chain"), rs.getString("status"), rs.getString("project_id"), rs.getString("store"),
                        rs.getTimestamp("loop_confirmed_at") != null));
        Map<String, List<String>> merchantProjects = new HashMap<>();
        return rows.stream().map(row -> {
            boolean customer = "facility".equals(row.chain());
            List<String> projects = customer ? (row.projectId() == null ? List.of() : List.of(row.projectId()))
                    : merchantProjects.computeIfAbsent(row.store(), platformRepository::listMerchantOperatingProjectIds);
            String label = (customer ? CustomerComplaintService.STATUS_LABELS : MERCHANT_STATUS_LABELS).getOrDefault(row.status(), row.status());
            return new Complaint(customer, label, projects, row.store(), row.loopConfirmed());
        }).toList();
    }

    /** 笔数、类型（消费客人投诉、商户投诉）、处理进度（已关闭在前，其余按状态分列）与闭环情况（经客户关怀部确认的才算闭环）。 */
    private static String body(List<Complaint> complaints) {
        if (complaints.isEmpty()) {
            return "本周无客诉。";
        }
        long customer = complaints.stream().filter(Complaint::customer).count();
        long loop = complaints.stream().filter(Complaint::loopConfirmed).count();
        Map<String, Long> progress = new LinkedHashMap<>(Map.of("已关闭", 0L));
        complaints.forEach(complaint -> progress.merge(complaint.statusLabel(), 1L, Long::sum));
        return "本周客诉共 " + complaints.size() + " 笔。类型：消费客人投诉 " + customer + "、商户投诉 " + (complaints.size() - customer)
                + "。处理进度：" + progress.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining("、"))
                + "。闭环情况：已闭环 " + loop + "、未闭环 " + (complaints.size() - loop) + "（客诉须经客户关怀部确认后方可视为形成闭环）。";
    }
}
