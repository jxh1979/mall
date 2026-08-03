package com.complex.platform.common;

import java.time.ZoneId;

/**
 * 所在地时区单点（2026-07-22 NEW-03 / 2026-08-02 用户拍板）。业务人员与客人一律按北京日历日
 * 理解业务日期——合同到期日、考勤日、按日查账那个「日」；而服务器时区是 America/Los_Angeles、
 * 旧代码多处按 UTC 折算日历日，两种取法都比北京日历日晚（UTC 晚 8 小时、服务器本地晚 15/16 小时），
 * 于是北京 8/3 凌晨发生的事被记到 8/2，业务人员翻台账对不上。
 *
 * <p>凡是把时刻（Instant）折成业务日历日、或取「今天」的地方一律走本常量，不要各自写 ZoneOffset.UTC。
 *
 * <p>不做成配置项：本项目只在一地经营，时区恒定，可配置的时区是没人会去设的配置。
 * 将来真要分地区，改的是调用方（按门店取所在地），不是给这里加一个 yml 开关。
 */
public final class LocalZone {

    /** 所在地（北京）时区：Instant → 业务日历日的唯一基准。 */
    public static final ZoneId ID = ZoneId.of("Asia/Shanghai");

    private LocalZone() {
    }
}
