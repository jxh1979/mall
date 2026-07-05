-- 前端修复批：T3-A 只读经营分析「客户意向」样例文案中文化。
-- 源为 V017 种子行 intent_p0.summary（原英文 customer asks for refund，随 AI 摘要样例落库），
-- 前端按原文渲染 → 违反全中文口径。此处仅更新该一条样例数据，不改结构、不改其他行。
UPDATE analytics_customer_intents
   SET summary = '客人申请退款'
 WHERE intent_id = 'intent_p0'
   AND summary = 'customer asks for refund';
