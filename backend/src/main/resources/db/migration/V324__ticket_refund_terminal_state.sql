-- V324 退款状态机终态(审计 AUD-018)：财务/分析此前把 ticket_refunds 全部状态(含 requested/rejected)
--   按 created_at 一律当成"已退款"扣减收入 —— requested(仅申请)/rejected(已驳回)/failed/cancelled 从未真正退款,
--   却提前/永久冲减了 revenue.net、日结与商户净额(FinanceJdbcIntegrationTests 还把 requested 计入写成绿灯)。
--   修复:①落地权威枚举 CHECK(HTML 枚举表:已申请/审批中/已通过/已驳回/处理中/成功/失败/已取消);
--         ②加生效时间列 succeeded_at;财务/分析仅统计有效状态及其生效时间(见下 §口径)。
--
--   ⚠️有效状态口径(交接主会话/用户裁决):本系统打款为存根(无真实支付网关),核销退款在"已通过(approved)"
--      即完成不可逆兑付副作用(冻结权益/回补名额/发打款指令),故本轮把"已通过 approved"迁为终态"成功 success"、
--      succeeded_at 记生效时刻;财务/分析仅统计 status='success' 且按 succeeded_at 落窗。存量 approved 行历史上即
--      有效(finalize 已执行兑付),故一并回填为 success。若业务要区分"批准=确认负债 vs 成功打款",须先改 HTML 口径。

ALTER TABLE ticket_refunds
    ADD COLUMN IF NOT EXISTS succeeded_at TIMESTAMPTZ;

-- 存量 approved(历史有效退款)→ success,生效时间取审批时间(无则更新时间)。
UPDATE ticket_refunds
   SET status = 'success',
       succeeded_at = COALESCE(approved_at, updated_at)
 WHERE status = 'approved';

-- 权威枚举 CHECK(HTML 退款单状态八值);此前无 CHECK,自由字符串任意可写。
ALTER TABLE ticket_refunds DROP CONSTRAINT IF EXISTS chk_ticket_refunds_status;
ALTER TABLE ticket_refunds ADD CONSTRAINT chk_ticket_refunds_status
    CHECK (status IN ('requested','review','approved','rejected','processing','success','failed','cancelled'));

-- 有效退款聚合索引:按生效时间取当日/当窗成功退款。
CREATE INDEX IF NOT EXISTS idx_ticket_refunds_success
    ON ticket_refunds (succeeded_at)
 WHERE status = 'success';
