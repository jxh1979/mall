-- AUD-015（P1）：历史 status=active 票种一次性归位。
-- 建票链已改为固定 draft→绑权益→发布闸（校验≥1个权益绑定+理由）→published；
-- 客户端公开列表与下单只认 published。历史 active 行按是否已绑权益归位：
--   有权益绑定 → published（视同已过发布闸，继续可售）；
--   无权益绑定 → draft（不可售——这正是"可售但无权益、收钱不出票"的隐患行，须补绑后重新发布）。
UPDATE ticket_types SET status = 'published'
WHERE status = 'active'
  AND id IN (SELECT DISTINCT ticket_type_id FROM ticket_type_entitlements);

UPDATE ticket_types SET status = 'draft' WHERE status = 'active';
