-- V385：权限展示名全中文
--
-- 违反：业务 HTML「展示给用户的一切文字必须全部中文」。permissions.permission_name 是
-- 权限模板页（PermissionTemplatesPage）直接上屏给业务人员勾选的展示文字，前端无翻译层。
-- 386 条里有 18 条还是英文（V255/V277/V287/V301/V334/V339 建表时播的英文名，V382 只是
-- 英文改英文），配权限的人在后台看到的是 "View channel mappings (store & ticket type)"。
--
-- 已执行的历史迁移不回改，向前修一次到位；permission_code 是机读标识保持不变。

UPDATE permissions SET permission_name = '查看酒吧内部结算价（成本价）'
 WHERE permission_code = 'bar.internal_settlement_price.view';

UPDATE permissions SET permission_name = '发起超范围经营申请'
 WHERE permission_code = 'contract.scope_request.raise';

UPDATE permissions SET permission_name = '管理券台账（建券种、发券、作废）'
 WHERE permission_code = 'customer.voucher.manage';

UPDATE permissions SET permission_name = '核销券（店内抵扣、凭券提货）'
 WHERE permission_code = 'customer.voucher.redeem';

UPDATE permissions SET permission_name = '查看券台账（券种、已发券、核销记录）'
 WHERE permission_code = 'customer.voucher.view';

UPDATE permissions SET permission_name = '管理商户费用台账（手工登记、冲正、自动挂账）'
 WHERE permission_code = 'finance.merchant_expense.manage';

UPDATE permissions SET permission_name = '查看商户费用台账与账期汇总'
 WHERE permission_code = 'finance.merchant_expense.view';

UPDATE permissions SET permission_name = '维护渠道映射（门店与票种）'
 WHERE permission_code = 'integration.channel_store_map.manage';

UPDATE permissions SET permission_name = '查看渠道映射（门店与票种）'
 WHERE permission_code = 'integration.channel_store_map.view';

UPDATE permissions SET permission_name = '查看库存成本台账（移动加权平均成本）'
 WHERE permission_code = 'inventory.cost.view';

UPDATE permissions SET permission_name = '管理平台供货主体'
 WHERE permission_code = 'inventory.supply_entity.manage';

UPDATE permissions SET permission_name = '查看平台供货主体'
 WHERE permission_code = 'inventory.supply_entity.view';

UPDATE permissions SET permission_name = '管理跨主体调拨单'
 WHERE permission_code = 'inventory.transfer_order.manage';

UPDATE permissions SET permission_name = '查看跨主体调拨单'
 WHERE permission_code = 'inventory.transfer_order.view';

UPDATE permissions SET permission_name = '查看联系电话明文（商户、供应商、招商线索、费用收款人、入场登记）'
 WHERE permission_code = 'pii.contact_phone.view';

UPDATE permissions SET permission_name = '审批删除票务权益模板'
 WHERE permission_code = 'ticket.entitlement.approve';

UPDATE permissions SET permission_name = '查看门票转赠记录'
 WHERE permission_code = 'ticket.transfer.read';

UPDATE permissions SET permission_name = '查看券内部结算价（成本价）'
 WHERE permission_code = 'voucher.internal_price.view';

-- 门禁：改完不允许再有纯 ASCII 展示名残留（新播权限忘了写中文名会在此炸出来）。
DO $$
DECLARE leftover text;
BEGIN
    SELECT string_agg(permission_code, ', ' ORDER BY permission_code) INTO leftover
      FROM permissions
     WHERE permission_name !~ '[^\x00-\x7F]';
    IF leftover IS NOT NULL THEN
        RAISE EXCEPTION '权限展示名必须全中文，以下权限仍是英文：%', leftover;
    END IF;
END $$;
