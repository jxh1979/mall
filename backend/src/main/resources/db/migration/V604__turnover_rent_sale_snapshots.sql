-- 文件版本：V0.01
-- 第十八批 U 段第 155 条 155-R1：营业额抽成逐笔销售快照（原账期、所适用档位费率逐段拆分、原月是否按保底收租）。
-- 依据：ENUM:257「冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回，不按冲正发生当期的档位与费率重算，
-- 原账期已定格的账单与档位不因冲正而重算；多笔退款逐笔分别冲正，不合并折算。」；DEC-R8-54「实现须为每笔计入抽成基数的销售保存其原账期
-- 与所适用档位费率（超额累进下逐段拆分）」；第十八批裁决 36「原月按保底收租的，该月销售跨月退款不冲回；原月按抽成额收租的，照档位费率冲回」。
-- 出账同一事务写入，一笔销售只一行（同期重出不覆盖）；跨期退款按 (source_kind, source_id) 取所退那笔的快照冲回。
CREATE TABLE turnover_rent_sale_snapshots (
    source_kind VARCHAR(16) NOT NULL,
    source_id VARCHAR(80) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    period CHAR(7) NOT NULL,
    net_amount_cent BIGINT NOT NULL,
    segments JSONB NOT NULL,
    billed_by_minimum BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (source_kind, source_id)
);
COMMENT ON TABLE turnover_rent_sale_snapshots IS '营业额抽成逐笔销售快照：该笔在原账期当月累计中的位置所适用的档位费率分段，跨期退款按此原额冲回';
COMMENT ON COLUMN turnover_rent_sale_snapshots.source_kind IS '来源类型：bar_bill 酒吧整单商品实收；reflow_order 餐饮、零售第三方回流订单';
COMMENT ON COLUMN turnover_rent_sale_snapshots.period IS '原账期（YYYY-MM）';
COMMENT ON COLUMN turnover_rent_sale_snapshots.net_amount_cent IS '该笔计入原账期抽成基数的净额（已减同期退款）';
COMMENT ON COLUMN turnover_rent_sale_snapshots.segments IS '逐段拆分 [{rate_bps, amount_cent}]，合计等于净额';
COMMENT ON COLUMN turnover_rent_sale_snapshots.billed_by_minimum IS '原月按保底收租（规则保底生效，或取高合同抽成额低于月租）：该月销售跨月退款不冲回';

-- 第十八批 U 段第 155 条 155-R3：餐饮、零售第三方回流退款带所退原订单的外部单号（列宽同 external_business_id，V030），营业额抽成跨期退款按原订单的
-- 原账期快照冲回。依据：ENUM:257「不论收款走平台还是走商户自有渠道、不论线上线下均计入」「多笔退款逐笔分别冲正，不合并折算。」；BSM:1330「连接第三方接入，
-- 餐饮和零售商户的 POS 结果回流到平台。」。原订单经既有唯一约束 uq_integration_order_syncs_external (provider_code, business_type, sync_type,
-- external_business_id) 查到，不另加索引。
ALTER TABLE integration_order_syncs ADD COLUMN original_external_business_id VARCHAR(128);
COMMENT ON COLUMN integration_order_syncs.original_external_business_id IS '退款所退原订单的外部单号（同渠道、同业务类型原订单的 external_business_id；餐饮、零售退款必填）';
