-- 文件版本：V0.01
-- 第 77 条 77-R1（BSM:1382「按响应时限和处理时限统计时效，超时自动升级通知主管。」；BSM:1385「超时自动升级通知主管」「维修处理与验收未完成的，
-- 继续按原工单时效升级跟进」；ENUM:265「响应时限、处理时限；超时自动升级通知主管」）：物业工单响应超时、处理超时各升级通知主管一次，
-- 分别记升级时刻作防重（只由升级作业与「标记逾期」按条件写入，不进工单记录、不对外返回）。
ALTER TABLE facility_work_orders ADD COLUMN response_escalated_at TIMESTAMPTZ;
ALTER TABLE facility_work_orders ADD COLUMN handle_escalated_at TIMESTAMPTZ;
