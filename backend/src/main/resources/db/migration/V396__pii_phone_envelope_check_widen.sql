-- V396 审计运行 20260902T183912Z · M009 A1-02：手机号密文列信封 CHECK 补齐。
--
-- 治理地图 PGM-1.15.2「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」。
-- 下列五列自 ISO-GAP-06 起已按 PiiCipher 加密落库，但此前既不在 tools/verify-pii-encryption.mjs 登记表、
-- 也没有 enc:v1: 信封 CHECK（V249/V252/V339 只覆盖 9 列）——任何写入点改动都可能静默落明文。
--   存酒三列写入点在仓储层（JdbcInventoryRepository），同批补入登记表；
--   两张转赠表在服务层加密（TicketTransferService / VoucherTransferService），登记表的仓储层正则无法覆盖，只能靠库上 CHECK。
--
-- 前置：两库五列现有行全部为 enc:v1: 信封（2026-09-07 于 160 实查：complex 7/7/2/0/0 行，明文 0 行），无需回填。
ALTER TABLE saved_bottles DROP CONSTRAINT IF EXISTS chk_saved_bottles_customer_phone_pii;
ALTER TABLE saved_bottles
    ADD CONSTRAINT chk_saved_bottles_customer_phone_pii
    CHECK (customer_phone IS NULL OR customer_phone = '' OR customer_phone LIKE 'enc:v1:%');

ALTER TABLE saved_bottles DROP CONSTRAINT IF EXISTS chk_saved_bottles_transfer_to_phone_pii;
ALTER TABLE saved_bottles
    ADD CONSTRAINT chk_saved_bottles_transfer_to_phone_pii
    CHECK (transfer_to_phone IS NULL OR transfer_to_phone = '' OR transfer_to_phone LIKE 'enc:v1:%');

ALTER TABLE saved_bottle_transfers DROP CONSTRAINT IF EXISTS chk_saved_bottle_transfers_to_phone_pii;
ALTER TABLE saved_bottle_transfers
    ADD CONSTRAINT chk_saved_bottle_transfers_to_phone_pii
    CHECK (to_phone IS NULL OR to_phone = '' OR to_phone LIKE 'enc:v1:%');

ALTER TABLE ticket_transfers DROP CONSTRAINT IF EXISTS chk_ticket_transfers_receiver_phone_pii;
ALTER TABLE ticket_transfers
    ADD CONSTRAINT chk_ticket_transfers_receiver_phone_pii
    CHECK (receiver_phone IS NULL OR receiver_phone = '' OR receiver_phone LIKE 'enc:v1:%');

ALTER TABLE voucher_transfers DROP CONSTRAINT IF EXISTS chk_voucher_transfers_receiver_phone_pii;
ALTER TABLE voucher_transfers
    ADD CONSTRAINT chk_voucher_transfers_receiver_phone_pii
    CHECK (receiver_phone IS NULL OR receiver_phone = '' OR receiver_phone LIKE 'enc:v1:%');
