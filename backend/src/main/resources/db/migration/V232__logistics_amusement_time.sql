-- V232 强制口径「物品履约有时间属性的环节都记时间 + 游乐核销记时间与场次」(补时间第2批,2026-07-05 用户拍板)。
--   语义与 V231 一致:只补「事件发生的当时时刻」,与下单时间(created_at)严格区分;历史留 null,严禁拿 created_at 冒充。

-- ① 酒吧出品配送:bar_order_items 已有 delivery_status(pending→delivered/voided)+created_at/updated_at,
--    但「送达/配送到位」这一步只翻状态不记独立时刻(updated_at 是通用戳,撤单等也会动,不能当送达时刻)。
--    补 delivered_at=deliverOrder 把该明细置 'delivered' 的当时时刻。
--    说明:该域出品与配送到位在 deliverOrder 一步完成(明细无独立 dispatched 中间态,备餐/出品进度在
--    订单级 status=accepted/preparing/ready 与出品工单 production_tickets 追踪),故本迁移只加 delivered_at,不加 dispatched_at。
ALTER TABLE bar_order_items
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ;

-- ② 历史回填(仅确定来源):已送达明细(delivery_status='delivered')的送达时刻,取自既有 updated_at。
--    口径依据:该域「送达=deliverOrder 唯一一次把明细置 delivered 并同步 updated_at=now」,故对
--    仍为 delivered(未被后续撤单改写 updated_at)的行,updated_at 即当次送达时刻,属确定来源、非冒充。
--    voided/pending 行不回填(未送达),留 null。
UPDATE bar_order_items
   SET delivered_at = updated_at
 WHERE delivery_status = 'delivered'
   AND delivered_at IS NULL
   AND updated_at IS NOT NULL;

-- ③ 游乐核销「对应场次」:游乐权益核销的正路是 amusement.verification.execute →
--    amusement_verifications(已含 session_id 对应场次 + created_at 核销时间,V014 建表既有),口径已满足,无需加列。
--    通用票务核销主链 ticket_verifications 属「票务入场核销」,其场次概念是 venue_sessions(营业场次,per_session 入场留痕
--    已落 entitlement_entry_records),与游乐场次(amusement_sessions,项目级容量场次)是两套资源、无既有映射;
--    从票务核销上下文反推 amusement_sessions 会造成错误关联,故本迁移不在 ticket_verifications 冒充游乐场次(详见报告设计分叉)。
