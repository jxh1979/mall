-- AUD-015 复核补刀（P2）：V293 只归位了 status=active 的历史行（生产实为 0 行），
-- 而真正在售的「可售但无权益」隐患是已 published 且零权益绑定的存量票种（生产核出 6 个，
-- 均为 2026-07-08 建的日间通票）——发布闸只拦「发布动作」，拦不住既成事实的存量行。
-- 统一归位：published 且无任何权益绑定 → draft（下架，补绑权益后经发布闸重新上架），
-- 与「客户列表/下单只认 published、发布须≥1 绑定」的口径闭环。
UPDATE ticket_types SET status = 'draft'
WHERE status = 'published'
  AND id NOT IN (SELECT DISTINCT ticket_type_id FROM ticket_type_entitlements);
