-- V365 通票分账瀑布重建 S2(综合服务费:换维 + 覆盖面 + 归属)·换维部分。
--   comprehensive_service_fee_configs 的第三维:「权益类型 entitlement_type」→「票种 ticket_type_id」。
--
-- 【业务依据】(最终解释权=三份业务 HTML,逐条对齐、不自造口径)
--   business-system-map-for-operations §「门票综合服务费」:
--     「所有门票项目都要收一道综合服务费(也叫项目管理费、通票分账中的娱乐平台公司留存……
--       分账瀑布中只扣一次)。按比例或固定金额收取,具体由项目与商户洽谈、后台配置;
--       ……可按（项目 × 商户 × 票种）范围另设每月封顶金额」。
--   → 权威维度的第三维是【票种】——业务人员在票务后台自己建的那一个个门票商品
--     (星海通票 / 单场演出票 / 游乐日票…),不是【权益类型】(bar / amusement / dining 这类系统内部分类)。
--
-- 【为什么必须换,不是改个名】(站业务人员使用角度)
--   ① 谈费率是按票种谈的:招商/运营跟商户谈的是「这个票种收几个点」,不会跟商户谈「酒水类权益收几个点」。
--      配置面的维度和谈判的维度不一致,填表的人每次都要在脑子里做一次翻译,填错了没人拦。
--   ② 对账对不上:一张通票里同时挂着酒水权益和游乐权益,按权益类型配费率 = 同一张票被按两套费率各抽一道。
--      业务人员在结算单上想回答「这张票到底收了多少综合服务费」时,拿票种去查根本查不出来。
--   ③ 前端文案本来就写的是「按『项目 × 商户 × 票种』配置」(SettlementConfigPage.tsx),
--      而底下存的是权益类型 —— 界面说一套、库里存一套,谁改谁踩坑。
--
-- 【零金额影响】开发库现存唯一一行是 csfc_default(project/merchant/权益类型 三维全空、fee_bps=0、
--   无固定额、无封顶)。换维后它仍是三维全空的全局默认行 → 解析结果不变 → 每笔费恒 0 → 分账逐分不变。
--
-- 【不做兼容,旧维度行一律 fail-loud 阻断】按权益类型配出来的行,换维后没有等价的票种可映射
--   (一个权益类型横跨多个票种)。静默保留 = 把它当成「所有票种通用」= 凭空【扩大】收费范围 = 多收钱。
--   故此处直接阻断:库里只要还有这种行,迁移就响亮失败,由人按票种在后台重配、把旧行下线后再跑
--   (与「宁可阻断不可近似」同一底线;开发库 / 全新测试库命中 0 行,天然幂等)。

DO $$
DECLARE stale_rows INT;
BEGIN
    SELECT count(*) INTO stale_rows
      FROM comprehensive_service_fee_configs
     WHERE entitlement_type IS NOT NULL;
    IF stale_rows > 0 THEN
        RAISE EXCEPTION '综合服务费配置里还有 % 行是按「权益类型」配的费率。换维到「票种」没有等价映射,'
                        '直接保留会把它当成「所有票种通用」、扩大收费范围。'
                        '请先在结算配置中心按票种重新配置并把旧行下线,再执行本迁移。', stale_rows;
    END IF;
END $$;

-- ① 新维度列:票种。NULL = 该(项目 × 商户)范围内所有票种通用,与原来 entitlement_type 留空的语义同构。
ALTER TABLE comprehensive_service_fee_configs
    ADD COLUMN IF NOT EXISTS ticket_type_id VARCHAR(64);

-- 真 FK(该表族已有先例:ticket_types.bar_id → merchants):防止后台把不存在的票种写进费率规则,
-- 那种规则永远命中不了、却会让业务人员以为已经配上了。
ALTER TABLE comprehensive_service_fee_configs DROP CONSTRAINT IF EXISTS fk_csfc_ticket_type;
ALTER TABLE comprehensive_service_fee_configs
    ADD CONSTRAINT fk_csfc_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id);

COMMENT ON COLUMN comprehensive_service_fee_configs.ticket_type_id IS
    '票种 id(NULL=该项目/商户范围内所有票种通用)。权威维度=业务人员在票务后台建的票种,不是权益类型';

-- ② 旧维度列下线(上面已 fail-loud 保证不含任何在用的按权益类型配置)。
ALTER TABLE comprehensive_service_fee_configs DROP COLUMN IF EXISTS entitlement_type;

-- ③ 唯一索引换维(沿用 V329 草案共存范式:同 scope 的草案与正式各一行)。
DROP INDEX IF EXISTS uq_csfc;
CREATE UNIQUE INDEX IF NOT EXISTS uq_csfc
    ON comprehensive_service_fee_configs (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        COALESCE(ticket_type_id, '__ANY__'),
        is_draft
    ) WHERE status = 'active';

-- ④ 按票种查费率的辅助索引(读侧解析 WHERE ticket_type_id IS NULL OR ticket_type_id = ?)。
CREATE INDEX IF NOT EXISTS idx_csfc_ticket_type
    ON comprehensive_service_fee_configs (ticket_type_id)
    WHERE ticket_type_id IS NOT NULL;

COMMENT ON TABLE comprehensive_service_fee_configs IS
    '门票综合服务费费率规则(= 娱乐平台公司留存 / 项目管理费,同一项只扣一次)。'
    '维度=项目 × 商户 × 票种(留空=该层通用);比例或固定额二选一,可另设每月封顶;所有门票项目都要收这一道';
