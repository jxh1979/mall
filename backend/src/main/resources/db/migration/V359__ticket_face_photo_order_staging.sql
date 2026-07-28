-- V359 游乐票人脸照：客人端下单拍照 → 出票前的「订单暂存位」（补齐 C1 前端缺口的唯一后端缺件）。
--
-- 【为什么需要这一格】V229 把人脸照绑在**已发权益**上（entitlement_id 唯一）。但本仓的出票只有一条路——
--   后台 `POST /ticketing/orders/{order_id}/issue`（权限 ticket.ticket.issue）。客人在 H5 上买票时权益还不存在，
--   客人也永远调不到出票端点。所以「客人自己拍的脸照」在下单和出票之间没有任何落脚点，C1 无法只改前端。
--   本迁移给同一张表加「订单位」：下单时按 (order_id, slot_index) 暂存密文，出票时取出转绑到权益、删暂存。
--
-- 【为什么不新建一张表】暂存照和正式照是同一份生物识别数据、同一套加密（PiiCipher AES-GCM）、同一个销毁
--   口径（purge_after + TicketFacePhotoPurgeJob 硬删）。分两张表 = 两套清理、两处漏一处就是 PIPL 事故。
--   一张表两种键位（权益位 / 订单位）比两张表少一半出事面。
--
-- 【暂存的销毁】暂存行 purge_after = 下单日 + 人脸照留存天数（后台 ADM-ANA-011 可配，默认 30 天）。
--   正常路径出票即删；订单没付/被取消/一直不出票的，既有清理 job 照 purge_after 硬删，不会永久躺着。
--
-- 【本迁移不做的事】不改任何既有行为：老的权益位行（entitlement_id 非空、order_id 空）读写路径一字未动。

-- ① 订单位两列：order_id + slot_index（第几张票的照片，从 0 起）。权益位行两列均为 NULL。
ALTER TABLE ticket_face_photos
    ADD COLUMN IF NOT EXISTS order_id    VARCHAR(64),
    ADD COLUMN IF NOT EXISTS slot_index  SMALLINT;

-- ② entitlement_id 放开 NOT NULL：暂存时权益还没发放，没有 id 可填。
--    既有 UNIQUE(entitlement_id) 约束在 PostgreSQL 下允许多个 NULL，权益位「一权益一照」不受影响。
ALTER TABLE ticket_face_photos
    ALTER COLUMN entitlement_id DROP NOT NULL;

-- ③ 两种键位二选一，不允许既不挂权益也不挂订单的孤儿行（写错立刻炸，别等到核销才发现没照）。
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_tfp_owner_present') THEN
        ALTER TABLE ticket_face_photos ADD CONSTRAINT chk_tfp_owner_present
            CHECK (entitlement_id IS NOT NULL OR (order_id IS NOT NULL AND slot_index IS NOT NULL));
    END IF;
END $$;

-- ④ 订单位唯一：同一订单同一张票只暂存一张照，重复提交覆盖不叠加。
--    不加 WHERE 条件（不做部分索引）：权益位行两列均为 NULL，PostgreSQL 唯一索引里 NULL 彼此不相等，
--    互不冲突；同时 ON CONFLICT (order_id, slot_index) 才能直接推断到这个索引。
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_tfp_order_slot') THEN
        ALTER TABLE ticket_face_photos ADD CONSTRAINT uq_tfp_order_slot UNIQUE (order_id, slot_index);
    END IF;
END $$;
