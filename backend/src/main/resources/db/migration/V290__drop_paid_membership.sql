-- V290 付费会员彻底废除（AUD-063/064，2026-07-18 用户拍板）。
-- 权威业务地图只保留"免费忠诚度会员"（成长积分/等级/储值），付费会员（客人花钱买年卡/权益包）
-- 从代码/API/表/文档四层全部下线。付费会员开通链路为半成品（activate 无任何 HTTP 入口，
-- 支付渠道默认关、无真实厂商适配器），存量 6 档位 + 1 订阅 + 5 支付意图均为 2026-07-08
-- "穷尽填充空表"播种数据，同一测试客户，无真实付费客户、无真实资金、无退款义务。
--
-- finance_payment_intents 中 5 笔 membership_subscribe 测试意图：财务流水只增不删，原样保留，
-- business_order_type='membership_subscribe' 自此为历史孤儿类型。

-- 前置护栏：确认无真实付费会员数据（仅允许已知播种测试客户）。发现非播种记录即中止，转人工退款复核后再迁移。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM customer_memberships
         WHERE customer_id <> 'customer_19a724a455dd4acf94b723aa3fdd2449'
    ) THEN
        RAISE EXCEPTION 'unexpected real membership rows detected, manual refund review required before drop';
    END IF;
END $$;

-- 先删有外键的子表（customer_memberships.plan_id -> customer_membership_plans.id），再删父表。
DROP TABLE IF EXISTS customer_memberships;
DROP TABLE IF EXISTS customer_membership_plans;

-- 权限退役：解除角色绑定并把三条付费会员权限码标 retired（permissions 只增不删，留痕）。
DELETE FROM role_permissions
 WHERE permission_code IN (
        'customer.membership.view',
        'customer.membership.manage',
        'customer.membership.subscribe'
 );

UPDATE permissions
   SET status = 'retired'
 WHERE permission_code IN (
        'customer.membership.view',
        'customer.membership.manage',
        'customer.membership.subscribe'
 );
