-- 文件版本：V0.01
-- V631：第十八批 AP 段第 252 条 252-R1（存酒作废原因必填、落存酒行、进成功审计并在主管复核页显示）。
-- 依据：BSM:1940「因为高价值酒水报损、盘点差异、存酒作废、异常调拨都可能产生责任争议，必须记录原因、员工、时间、点位、照片、视频和主管复核结果。」
-- 列宽同契约 cancelSavedBottle.reason（maxLength 255）；研发阶段无存量（UD-07），不回填。
ALTER TABLE saved_bottles ADD COLUMN cancel_reason VARCHAR(255);
