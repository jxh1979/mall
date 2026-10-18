package com.complex.platform.analytics;

// 文件版本：V0.01

import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.common.api.ApiException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** 游乐指标按同一综合体版本取日期；平台跨域财务窗口不经过此读源。 */
@Component
public class AmusementBusinessDayAnalytics {
    private final NamedParameterJdbcTemplate jdbc;
    public AmusementBusinessDayAnalytics(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
    private record Definition(String table,String projectColumn,String fields,String condition,String summary,boolean locked,boolean balance,boolean capacity) {}
    private static Definition definition(String key) {
        String session="id, project_id AS \"projectId\", start_time AS \"startTime\", end_time AS \"endTime\", status, created_at AS \"createdAt\"";
        String booking="id, project_id AS \"projectId\", session_id AS \"sessionId\", booking_type AS \"bookingType\", customer_name AS \"customerName\", party_size AS \"partySize\", reserved_capacity AS \"reservedCapacity\", booking_status AS \"bookingStatus\", created_at AS \"createdAt\", paid_at AS \"paidAt\"";
        String verification="id, project_id AS \"projectId\", session_id AS \"sessionId\", direction, verification_result AS \"verificationResult\", verified_people AS \"verifiedPeople\", created_at AS \"createdAt\"";
        String inspection="id, project_id AS \"projectId\", device_id AS \"deviceId\", overall_result AS \"overallResult\", inspection_status AS \"inspectionStatus\", rectification_status AS \"rectificationStatus\", reopen_status AS \"reopenStatus\", created_at AS \"createdAt\"";
        return switch(key) {
            case "amusement.session_count" -> new Definition("amusement_sessions","project_id",session,"TRUE","COUNT(*)",true,false,false);
            case "amusement.session_capacity" -> new Definition("amusement_projects","id","id, id AS \"projectId\", project_name AS \"projectName\", capacity, open_status AS \"status\", created_at AS \"createdAt\"","TRUE","COALESCE(SUM(capacity),0)",false,false,true);
            case "amusement.group_booking_count" -> new Definition("amusement_group_bookings","project_id",booking,"TRUE","COUNT(*)",false,false,false);
            case "amusement.group_reserved_capacity" -> new Definition("amusement_group_bookings","project_id",booking,"booking_status IN ('approved','released')","COALESCE(SUM(reserved_capacity),0)",false,false,false);
            case "amusement.entry_count" -> new Definition("amusement_verifications","project_id",verification,"direction='entry' OR direction IS NULL","COUNT(*)",true,false,false);
            case "amusement.exit_count" -> new Definition("amusement_verifications","project_id",verification,"direction='exit'","COUNT(*)",true,false,false);
            case "traffic.amusement" -> new Definition("amusement_verifications","project_id",verification,"verification_result='success'","COUNT(*)",true,false,false);
            case "amusement.inspection_count" -> new Definition("amusement_safety_inspections","project_id",inspection,"TRUE","COUNT(*)",false,false,false);
            case "amusement.inspection_pending_rectification" -> new Definition("amusement_safety_inspections","project_id",inspection,"COALESCE(rectification_status,overall_result) IN ('pending','rectification_pending','pending_rectification')","COUNT(*)",false,true,false);
            default -> throw ApiException.badRequest("VALIDATION_FAILED","不支持的游乐指标");
        };
    }

    public AnalyticsRecords.MetricDetailRows detail(String key,LocalDate from,LocalDate to,Instant now,
            boolean allHistory,int limit,int offset,Collection<String> projects) {
        if(projects!=null && projects.isEmpty())return new AnalyticsRecords.MetricDetailRows(List.of(),0,BigDecimal.ZERO);
        Definition d=definition(key);
        Query query=query(d,from,to,now,allHistory,projects);
        MapSqlParameterSource p=query.params().addValue("limit",limit).addValue("offset",offset);
        requireComplete(query);
        List<Map<String,Object>> rows=jdbc.queryForList(query.sql()+" SELECT "+d.fields()+", analytics_complex_id AS \"complexId\", analytics_business_day AS \"businessDate\" FROM filtered ORDER BY created_at DESC,id LIMIT :limit OFFSET :offset",p);
        rows.forEach(row->row.replaceAll((k,v)->v instanceof Timestamp t?t.toInstant():v instanceof java.sql.Date date?date.toLocalDate():v));
        Map<String,Object> sums=jdbc.queryForMap(query.sql()+" SELECT count(*) AS count,"+d.summary()+" AS summary FROM filtered",p);
        return new AnalyticsRecords.MetricDetailRows(rows,((Number)sums.get("count")).longValue(),new BigDecimal(sums.get("summary").toString()));
    }

    public Map<Integer,Long> hourly(LocalDate date) {
        Query q=query(definition("traffic.amusement"),date,date,Instant.now(),false,null);
        requireComplete(q);
        Map<Integer,Long> result=new LinkedHashMap<>();
        jdbc.query(q.sql()+" SELECT date_part('hour',created_at AT TIME ZONE 'UTC')::int AS hour, count(*) AS total FROM filtered GROUP BY hour",q.params(),
                (org.springframework.jdbc.core.RowCallbackHandler)rs->result.put(rs.getInt("hour"),rs.getLong("total")));
        return result;
    }
    private record Query(String sql,MapSqlParameterSource params,boolean dated,boolean balance) {}
    private Query query(Definition d,LocalDate from,LocalDate to,Instant now,boolean allHistory,Collection<String> projects) {
        LocalDate low=from!=null?from:to,high=to!=null?to:from;
        boolean dated=!d.capacity() && (d.balance() || low!=null || !allHistory);
        MapSqlParameterSource p=new MapSqlParameterSource().addValue("all_projects",projects==null)
                .addValue("projects",projects==null?List.of(""):projects)
                .addValue("from_day",low,java.sql.Types.DATE).addValue("to_day",high,java.sql.Types.DATE)
                .addValue("now",now.atOffset(ZoneOffset.UTC));
        // 粗窗只排除不可能落入目标营业日的历史；真实分桶始终用所在地已批准版本。
        Instant roughStart=low==null?now.minusSeconds(86400L*3):low.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant roughEnd=high==null?now.plusSeconds(86400L*3):high.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant();
        p.addValue("rough_start",roughStart.atOffset(ZoneOffset.UTC)).addValue("rough_end",roughEnd.atOffset(ZoneOffset.UTC));
        String complex=d.locked()?"COALESCE(t.complex_id,m.complex_id)":"m.complex_id";
        String factTime="amusement_sessions".equals(d.table())?"t.start_time":"t.created_at";
        String day="(timezone(c.zone_id,"+factTime+")-make_interval(hours=>c.cutoff_hour))::date";
        if(d.locked())day="COALESCE(t.business_date,"+day+")";
        String roughWindow=factTime+">=:rough_start AND "+factTime+"<:rough_end";
        // 已锁营业日是实际开场原件，不能因提前创建或后续时间差被粗窗剔除。
        String sourceFilter=dated&&!d.balance()?" AND "+(d.locked()
                ? "(t.business_date IS NOT NULL OR ("+roughWindow+"))":roughWindow):"";
        String sql="""
            WITH source AS (
              SELECT t.*, %s AS analytics_complex_id, %s AS analytics_business_day,
                (timezone(n.zone_id,:now)-make_interval(hours=>n.cutoff_hour))::date AS current_business_day,
                n.zone_id AS current_zone_id,n.cutoff_hour AS current_cutoff_hour
              FROM %s t LEFT JOIN (%s) m ON m.amusement_project_id=t.%s
              LEFT JOIN LATERAL (SELECT zone_id,cutoff_hour FROM amusement_business_day_configs
                WHERE project_id=%s AND status='approved' AND effective_at<=%s ORDER BY effective_at DESC LIMIT 1) c ON TRUE
              LEFT JOIN LATERAL (SELECT zone_id,cutoff_hour FROM amusement_business_day_configs
                WHERE project_id=%s AND status='approved' AND effective_at<=:now ORDER BY effective_at DESC LIMIT 1) n ON TRUE
              WHERE (%s)%s
            ), scoped AS (
              SELECT * FROM source WHERE :all_projects OR analytics_complex_id IN (:projects)
            )
            """.formatted(complex,day,d.table(),AmusementBusinessDayService.COMPLEXES_SQL,d.projectColumn(),complex,factTime,complex,d.condition(),sourceFilter);
        String predicate="TRUE";
        if(d.balance()) {
            // 截至日的窗末可跨版本；按每版范围裁剪，再取该当地日期实际存在的最后边界。
            sql+="""
                , target_windows AS (
                  SELECT s.*,
                    (SELECT max(least(bound.natural_end,COALESCE(bound.next_at,'infinity'::timestamptz)))
                       FROM (
                         SELECT v.effective_at,lead(v.effective_at) OVER(ORDER BY v.effective_at) AS next_at,
                           ((COALESCE(:to_day,s.current_business_day)+1)::timestamp+make_interval(hours=>v.cutoff_hour)) AT TIME ZONE v.zone_id AS natural_end,
                           (COALESCE(:to_day,s.current_business_day)::timestamp+make_interval(hours=>v.cutoff_hour)) AT TIME ZONE v.zone_id AS natural_start
                         FROM amusement_business_day_configs v WHERE v.project_id=s.analytics_complex_id AND v.status='approved'
                       ) bound
                       WHERE bound.natural_end>bound.effective_at AND (bound.next_at IS NULL OR bound.natural_start<bound.next_at)
                    ) AS target_end
                  FROM scoped s
                )
                """;
            predicate="created_at<target_end";
        } else if(dated)predicate="analytics_business_day>=COALESCE(:from_day,current_business_day) AND analytics_business_day<=COALESCE(:to_day,current_business_day)";
        sql+=", filtered AS (SELECT * FROM "+(d.balance()?"target_windows":"scoped")+" WHERE "+predicate+")";
        return new Query(sql,p,dated,d.balance());
    }
    private void requireComplete(Query query) {
        String invalid="analytics_complex_id IS NULL";
        if(query.dated())invalid+=query.balance()?" OR target_end IS NULL":" OR analytics_business_day IS NULL OR (:from_day::date IS NULL AND current_business_day IS NULL)";
        Integer missing=jdbc.queryForObject(query.sql()+" SELECT count(*) FROM "+(query.balance()?"target_windows":"scoped")+" WHERE "+invalid,query.params(),Integer.class);
        if(missing!=null && missing>0)throw ApiException.conflict("AMUSEMENT_BUSINESS_DAY_CONFIGURATION_REQUIRED","所选游乐数据缺少真实综合体或对应历史营业日配置，补齐后才能统计");
    }
}
