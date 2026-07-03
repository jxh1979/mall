-- 清单 P1-2:服务点单提成幂等——同一服务单至多一条提成,防并发双 key 完成/重放放大到薪资月结代扣个税。
-- 先清历史重复(保留最早一条),再建部分唯一索引;仅约束 service_order 来源,manual 等其他来源(source_id 可空/可重)不受影响。
DELETE FROM staff_commission_ledger a
 USING staff_commission_ledger b
 WHERE a.source_type = 'service_order'
   AND b.source_type = 'service_order'
   AND a.source_id = b.source_id
   AND a.source_id IS NOT NULL
   AND a.ctid > b.ctid;

CREATE UNIQUE INDEX IF NOT EXISTS uq_commission_service_order
    ON staff_commission_ledger (source_type, source_id)
    WHERE source_type = 'service_order';
