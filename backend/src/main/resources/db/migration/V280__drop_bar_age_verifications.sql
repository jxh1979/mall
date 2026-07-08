-- 废除酒吧年龄核验台账（2026-07-08 用户拍板）。
-- 该表自 V013 建立后从未被任何 service/controller 写入（仅 repository 层有 saveAgeVerification，无调用方=死写路径）；
-- 年龄合规由全场 18+ 人脸入场闸承担，独立台账不再需要。存储链路代码同步删除。
DROP TABLE IF EXISTS bar_age_verifications;
