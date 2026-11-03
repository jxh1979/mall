-- 文件版本：V0.01
-- 第十八批 O 段第 129 条 129-R1：下线终端离线核销同步通道，删除只服务这条通道的同步任务表。依据：BSM:800「……核销必须在线、不做离线核销。」；
-- BSM:891「……二维码原始值不存在手机本地，只在线核验。」。POST /api/v1/terminals/sync-tasks 与 GET /api/v1/terminals/offline-entitlements
-- 两接口及其代码已同批删除；该表在 V006 建、V039 只做过脱敏 UPDATE，没有外键指向它；研发期无存量（UD-07），直接删表（同 V274 范式）。
DROP TABLE IF EXISTS terminal_sync_tasks;
