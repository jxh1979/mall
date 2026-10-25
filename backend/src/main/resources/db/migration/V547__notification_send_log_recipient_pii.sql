-- 文件版本：V0.01
-- V547：短信发送留痕的收件人手机号加密存放（第十四批复核）。
-- 依据：PGM L876「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。」——notification_send_log.recipient 改落 enc:v1 信封密文
--   （JdbcNotificationRepository 写时 piiCipher.encrypt、读时 decrypt；发送器拿内存里的明文，下发不变）。
-- 存量不回填：NOT VALID 只守新写入。
ALTER TABLE notification_send_log
    ADD CONSTRAINT chk_notification_send_log_recipient_pii CHECK (recipient LIKE 'enc:v1:%') NOT VALID;
