package com.complex.platform.ticketing;

// 文件版本：V0.02

import com.complex.platform.common.BusinessDayResolver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 多人票首场次（父票 first_session_key）的结束时刻。场次键是 venue_sessions.id 时取该场营业日按营业时间表的实际闭店时刻（跨夜闭店落次日，
 * 票务 PRD「跨夜场次以该场次的实际闭店时间为准，不按自然日切分」，时区与场次键生成处一致：系统默认时区）；无场次配置时场次键是订单综合体营业日
 * （第十八批 AH 段 238-R1），结束于该营业日闭店时点（238-R2：BSM:800「一个场次＝一次开门到关门」，{@link BusinessDayResolver#closeAt}，所在地时区）。
 * 供到期结算作业（ENUM 第 48 行）与门票转赠场次限制（clients-R40，BSM 第 809 行）共用。
 */
public final class FirstSessionWindow {

    /** 父票首场次：label＝场次日期，end＝结束时刻（定不出为 null）。 */
    public record Session(String label, Instant end) {
    }

    /** 转赠向双方明示的场次限制：restricted＝本人份只能在同票已开启的场次内入场，ended＝该场次已结束。 */
    public record Limit(boolean restricted, String label, boolean ended) {
        public static final Limit NONE = new Limit(false, null, false);
    }

    private FirstSessionWindow() {
    }

    /** 定不出时返回 null，交票面到期处理。projectId＝订单综合体（营业日键按它的闭店时点，无版本取出厂）。 */
    public static Instant endOf(String firstSessionKey, LocalDate sessionDate, Integer openSecond, Integer closeSecond,
            BusinessDayResolver businessDays, String projectId) {
        ZoneId zone = ZoneId.systemDefault();
        if (sessionDate != null) {
            if (openSecond == null || closeSecond == null) {
                return null;
            }
            LocalDate day = closeSecond < openSecond ? sessionDate.plusDays(1) : sessionDate;
            return day.atTime(LocalTime.ofSecondOfDay(closeSecond)).atZone(zone).toInstant();
        }
        try {
            return businessDays.closeAt(projectId, null, LocalDate.parse(firstSessionKey)).toInstant();
        } catch (DateTimeParseException notADate) {
            return null;
        }
    }
}
