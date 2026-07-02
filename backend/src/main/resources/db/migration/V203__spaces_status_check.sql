-- BI S3(2026-07-02 拍板):铺位「不可租」标记防呆——status 仅允许 active/unavailable。
-- 六态租赁看板的人工态只认 unavailable(JdbcContractRepository.spaceRentalStatus),
-- 手抖填错字该铺位会悄悄漏回出租率分母;先归一存量脏值再上 CHECK。
UPDATE spaces SET status = 'active' WHERE status NOT IN ('active', 'unavailable');
ALTER TABLE spaces DROP CONSTRAINT IF EXISTS chk_spaces_status;
ALTER TABLE spaces ADD CONSTRAINT chk_spaces_status CHECK (status IN ('active', 'unavailable'));
