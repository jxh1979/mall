-- V353 游乐票人脸照留存天数改后台可配(用户 2026-07-28 拍板:"改成后台可配,默认 30 天")。
--   原:TicketFacePhotoService.RETENTION_DAYS_AFTER_VALID_TO = 30 硬编码,改留存期要改代码重新发版。
--   改:落 notification_windows(key=ticketing.face_photo_retention),经既有 NotificationWindowConfig 读,
--      缺配置兜底 30(行为不变),后台 ADM-ANA-011 直接改。
--
-- 【为什么放这张表,不另起炉灶】notification_windows 是本仓「按天计的运营参数」通用键值表(V219 建,
--   仿 operating_params 键值范式),已配套 NotificationWindowConfig(60s 缓存 + 缺配置回落)与后台维护页
--   ADM-ANA-011 + 审计事件 analytics.notification_window.updated。再建一张只放一个数字的表、一套端点、
--   一个页面,业务人员反而要多记一个入口。
-- 【为什么同时改页名】原页名「提醒窗配置」只讲提醒,合规口径的人是找不到「人脸照留存」的。本 PR 把页名
--   改成「提醒与留存天数配置」,并在 description 写明改大改小各是什么后果,让改这个数的人知道自己在改什么。
-- 【权限】沿用 platform.notification_window.manage(V219,只授 role_platform_admin,与企微留存
--   wecom.retention.manage 同一受众),不新增权限点;每次改动经 OperatingParamsService 写审计。
-- 【上界】本迁移给全表加 days <= 3650 上界(对齐 wecom_retention_policies 的 1..3650):留存类键管的是
--   个人信息销毁时点,没有上界等于允许事实上永久留存,与 PIPL「最短必要期」冲突。既有三个提醒键
--   (30/7/3)与本次 30 均远小于 3650,行为不变。

-- ① 种子 = 原硬编码值 30(行为不变)。
INSERT INTO notification_windows (window_key, days, description) VALUES
    ('ticketing.face_photo_retention', 30, '游乐票人脸照留存天数（票失效后满该天数自动销毁，涉个人信息保护）')
ON CONFLICT (window_key) DO NOTHING;

-- ② 天数上界 3650(先兜底把越界存量拉回上界,再加约束;开发库本无越界行,此处只为迁移可重跑不炸)。
UPDATE notification_windows SET days = 3650 WHERE days > 3650;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notif_window_days_max') THEN
        ALTER TABLE notification_windows ADD CONSTRAINT chk_notif_window_days_max CHECK (days <= 3650);
    END IF;
END $$;
