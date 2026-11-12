package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.evidence.RiskAlertService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AH 段第 218 条 218-R1（BSM:800「对高频出入场的须调看出入录像核查，以防止将票借给他人使用：一晚出入次数或连续两日出入合计次数达到后台配置的阈值
 * 即触发核查（业务参数，按项目可配，出厂默认一晚 4 次、连续两日合计 7 次，须同时配置上下限）；其中「一晚」指一个营业日……「连续两日」指连续两个营业日，
 * 均不按自然日切分；由系统自动预警，楼宇保安团队据预警调阅录像」「完成检票环节指其中二维码刷码核销成功的那一刻，与各酒吧入场、各游乐项目入场等场内单点
 * 核销相区别」；BSM:1657「预警只作提示与线索汇集，不自动阻断业务」）：综合体检票（入园权益）成功后，按同票同人份数订单综合体本营业日、连同上一营业日的
 * 检票成功次数（含同场次再入场；已冲正的不算；酒吧入场、游乐入场不计），达到该综合体阈值（无项目行取出厂行）即进统一风控预警队列。同人份同营业日同类只一条，
 * 入场照常。
 */
@Service
public class TicketFrequentEntryAlertService {

    static final String ALERT_TYPE = "ticket.frequent_entry";

    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessDayResolver businessDays;
    private final RiskAlertService riskAlerts;

    public TicketFrequentEntryAlertService(NamedParameterJdbcTemplate jdbc, BusinessDayResolver businessDays, RiskAlertService riskAlerts) {
        this.jdbc = jdbc;
        this.businessDays = businessDays;
        this.riskAlerts = riskAlerts;
    }

    /** 核销成功后在同一事务里调用；不是带父票的入园核销直接返回。 */
    void afterEntry(String verificationId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.parent_ticket_id, e.slot_index, o.project_id, v.created_at
                  FROM ticket_verifications v
                  JOIN issued_entitlements e ON e.id = v.entitlement_id
                  LEFT JOIN ticket_orders o ON o.id = e.order_id
                 WHERE v.id = :id AND v.status = 'success' AND e.entitlement_type = 'entry' AND e.parent_ticket_id IS NOT NULL
                """, Map.of("id", verificationId));
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String parent = (String) row.get("parent_ticket_id");
        int slot = ((Number) row.get("slot_index")).intValue();
        String project = (String) row.get("project_id");
        LocalDate day = businessDays.businessDate(project, null, ((Timestamp) row.get("created_at")).toInstant());
        Instant end = businessDays.end(project, null, day);
        Map<String, Object> setting = jdbc.queryForMap("""
                SELECT night_threshold, two_day_threshold FROM customer_frequent_entry_alert_settings
                 WHERE project_id IN (:project, '__global__') ORDER BY CASE WHEN project_id = :project THEN 0 ELSE 1 END LIMIT 1
                """, new MapSqlParameterSource("project", project));
        int nightThreshold = ((Number) setting.get("night_threshold")).intValue();
        int twoDayThreshold = ((Number) setting.get("two_day_threshold")).intValue();
        int night = entries(parent, slot, businessDays.start(project, null, day), end);
        int twoDay = entries(parent, slot, businessDays.start(project, null, day.minusDays(1)), end);
        if (night >= nightThreshold) {
            alert(parent, slot, day, "night", project, "该票第 " + slot + " 人份本营业日出入 " + night + " 次，达到一晚阈值 " + nightThreshold + " 次");
        }
        if (twoDay >= twoDayThreshold) {
            alert(parent, slot, day, "two_day", project,
                    "该票第 " + slot + " 人份连续两个营业日出入合计 " + twoDay + " 次，达到连续两日阈值 " + twoDayThreshold + " 次");
        }
    }

    private int entries(String parent, int slot, Instant from, Instant to) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM ticket_verifications v JOIN issued_entitlements e ON e.id = v.entitlement_id
                 WHERE e.parent_ticket_id = :parent AND e.slot_index = :slot AND e.entitlement_type = 'entry'
                   AND v.status = 'success' AND v.created_at >= :from AND v.created_at < :to
                """, new MapSqlParameterSource().addValue("parent", parent).addValue("slot", slot)
                .addValue("from", from.atOffset(ZoneOffset.UTC)).addValue("to", to.atOffset(ZoneOffset.UTC)), Integer.class);
        return count == null ? 0 : count;
    }

    /** 预警 id 列宽 64、父票 id 已 36 位：取「afe_」＋同人份同营业日同类摘要的 SHA-256 前 32 位，一类只一条。 */
    private void alert(String parent, int slot, LocalDate day, String kind, String project, String fact) {
        riskAlerts.recordSystemAlert("afe_" + sha256(parent + "|" + slot + "|" + day + "|" + kind).substring(0, 32), "ticketing", parent,
                ALERT_TYPE, "suspicious", fact + "，请楼宇保安团队调阅出入录像核查是否借票（只作提示，不阻断入场）", project, "system");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
