package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.LocalDate;

/**
 * 节假日日历端口（第十八批 AT 段 298-R1；BSM:776「按日期平日票、周末票、节假日票（基础价，优先级最高）。」）。
 *
 * <p>票务域声明、分析域实现（{@code JdbcAnalyticsRepository}，表 holiday_calendar，在经营参数页维护，BSM:1832），
 * 与 {@link ComboPriceToleranceConfigPort} 同一套跨域端口范式。
 */
public interface HolidayCalendarPort {

    /** 该日是否落在节假日日历任一放假区间内。 */
    boolean isWithinHoliday(LocalDate date);
}
