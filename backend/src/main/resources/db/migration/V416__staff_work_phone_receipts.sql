-- 文件版本：V0.01
ALTER TABLE staff_attendance_records ALTER COLUMN method TYPE VARCHAR(24);
ALTER TABLE staff_attendance_records DROP CONSTRAINT chk_att_method;
-- 历史手动记录仍可读取；新增打卡由服务仅接受工作手机事件。
ALTER TABLE staff_attendance_records ADD CONSTRAINT chk_att_method
    CHECK (method IN ('nfc', 'qr', 'manual', 'charging_disconnect'));
